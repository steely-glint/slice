/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidatePairState;
import com.ipseorama.slice.ORTC.enums.RTCIceTransportState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.stun.StunAttribute;
import com.ipseorama.slice.stun.StunBindingRequest;
import com.ipseorama.slice.stun.StunBindingTransaction;
import com.ipseorama.slice.stun.StunErrorTransaction;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunTransaction;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author tim
 */
public class RTCIceTransport {

    static int MAXCHECKS = 100;
    RTCIceGatherer iceGatherer;
    RTCIceRole role;
    final RTCIceComponent component;
    protected RTCIceTransportState state;
    RTCIceParameters remoteParameters;
    List<RTCIceCandidate> remoteCandidates;
    List<RTCIceCandidatePair> candidatePairs;
    RTCIceCandidatePair selectedPair;

    private final Comparator<RTCIceCandidatePair> ordering;
    private long tieBreaker;
    private StunTransactionManager transMan;
    private IceEngine ice;

    public List<RTCIceCandidate> getRemoteCandidates() {
        return remoteCandidates;
    }

    public RTCIceCandidatePair getSelectedCandidatePair() {
        return selectedPair;
    }

    public void start(RTCIceGatherer gatherer, RTCIceParameters remoteParameters, RTCIceRole role) {
        // for the moment we will ignore the new values and assume that the constructor was right....
        // and ignore the re-start semantics.
        ice = gatherer.getIceEngine();

        this.transMan = gatherer.getStunTransactionManager();
        transMan.setTransport(this);
        final EventHandler oldAct = gatherer.onlocalcandidate;
        if (remoteParameters != null) {
            this.remoteParameters = remoteParameters;
        }
        this.role = role;
        gatherer.onlocalcandidate = (RTCEventData c) -> {
            if (c instanceof RTCLocalIceReflexCandidate) {
                Log.debug("Not pairing local reflex candidate " + ((RTCLocalIceReflexCandidate) c).toString());
            } else {
                if (c instanceof RTCLocalIceCandidate) {
                    RTCLocalIceCandidate l = (RTCLocalIceCandidate) c;
                    List<RTCIceCandidate> remotes = new ArrayList(remoteCandidates);
                    for (RTCIceCandidate r : remotes) {
                        addPair(l, r); // actually adds pair to the ICE algo
                    }
                } else {
                    Log.warn("Ignoring candidate event " + c);
                }
            }
            if (oldAct != null) {
                oldAct.onEvent(c); // sends candidate to far side.
            }
        };

    }

    public void stop() {
    }

    public RTCIceParameters getRemoteParameters() {
        return remoteParameters;
    }

    public RTCIceParameters getLocalParameters() {
        return this.iceGatherer.getLocalParameters();
    }

    RTCIceTransport createAssociatedTransport() {
        return null; // don't do this - rtcp-mux is good.
    }

    public void addRemoteCandidate(RTCIceGatherCandidate remoteCandidate) {
        if (remoteCandidate instanceof RTCIceCandidate) {
            RTCIceCandidate r = (RTCIceCandidate) remoteCandidate;
            synchronized (remoteCandidate) {
                this.remoteCandidates.add(r);
            }
            List<RTCLocalIceCandidate> locals = new ArrayList(iceGatherer.getLocalCandidates());
            for (RTCLocalIceCandidate l : locals) {
                addPair(l, r);
            }
        } else {
            // assume end-of-candidates....
        }
    }

    public void setRemoteCandidates(List<RTCIceCandidate> remoteCandidates) {
        // assumption is that this blatts the existing list - so we need to
        // compare and selectively add/delete - I suppose?!?
        // ignore for now.
        // assume all candidates are added vi addRemoteCandidate ;-)
    }

    public EventHandler onstatechange;
    public EventHandler oncandidatepairchange;

    public RTCIceTransport(RTCIceGatherer ig,
            RTCIceRole r,
            RTCIceComponent comp) {
        this.iceGatherer = ig;
        SecureRandom rand = new SecureRandom();
        this.tieBreaker = rand.nextLong();
        this.role = r;
        this.component = comp;
        this.state = RTCIceTransportState.NEW;
        this.remoteCandidates = new ArrayList();
        this.candidatePairs = new ArrayList();
        this.ordering = (RTCIceCandidatePair p1, RTCIceCandidatePair p2) -> {
            long lcp = p1.priority(role) - p2.priority(role);
            int ret = 0;
            if (lcp > 0) {
                ret = -1;
            } else if (lcp < 0) {
                ret = 1;
            }
            return ret;
        };
    }

    /**
     * @param _state the _state to set
     */
    private void setState(RTCIceTransportState newstate) {
        this.state = newstate;
        if (this.onstatechange != null) {
            onstatechange.onEvent(newstate);
        }
    }

    private RTCIceCandidatePair addPair(RTCLocalIceCandidate l, RTCIceCandidate r) {
        RTCIceCandidatePair ret = null;
        if (state == RTCIceTransportState.NEW) {
            setState(RTCIceTransportState.CHECKING);
        }
        if ((l != null) && (r != null)) {
            if (l.getProtocol() == r.getProtocol() && l.getIpVersion() == r.getIpVersion()) {
                synchronized (candidatePairs) {
                    boolean present = candidatePairs.stream().anyMatch((RTCIceCandidatePair p) -> {
                        return (l.sameFoundation(p.getLocal()) && r.sameFoundation(p.getRemote()));
                    });
                    if (!present) {
                        RTCIceCandidatePair p = new RTCIceCandidatePair(l, r);
                        if (p != null) {
                            p.setState(RTCIceCandidatePairState.FROZEN);
                            ret = p;
                            candidatePairs.add(p);
                            p.onStateChange = (data) -> {
                                this.pairChangedState(p);
                            };
                            Log.debug("added candidate pair " + p.toString());
                        }
                    } else {
                        Log.debug("ignoring due to exisiting candidate pair " + r.toString() + " " + l.toString());
                    }
                }
            } else {
                Log.verb("ignoring incompatiple candidate pair " + r.toString() + " " + l.toString());
            }
        } else {
            Log.error("only half a pair of candidates to add " + l + " and " + r);
        }
        return ret;
    }

    public RTCIceCandidatePair nextCheck() {
        RTCIceCandidatePair ret = nextCheck(RTCIceCandidatePairState.WAITING);
        if (ret == null) {
            RTCIceCandidatePair defrost = nextCheck(RTCIceCandidatePairState.FROZEN);
            if (defrost != null) {
                defrost.setState(RTCIceCandidatePairState.WAITING);
                ret = defrost;
            }
        }
        return ret;
    }

    public RTCIceCandidatePair nextCheck(RTCIceCandidatePairState targetState) {
        Optional<RTCIceCandidatePair> ret = null;
        if (this.iceGatherer.getState() == RTCIceGathererState.COMPLETE) {
            synchronized (candidatePairs) {
                ret = candidatePairs.stream()
                        /*.filter((RTCIceCandidatePair icp) -> {
                        return !transMan.localIsBusy(icp); // dont list any where there is a transaction in flight, it confuses NAT
                    })*/
                        .filter((RTCIceCandidatePair icp) -> {
                            return icp.getState() == targetState;
                        })
                        .limit(MAXCHECKS)
                        .sorted(ordering)
                        .findFirst();
            }
        } else {
            Log.info("still gathering...");
        }
        return ((ret != null) && ret.isPresent()) ? ret.get() : null;
    }

    private boolean checkRoleOk(StunBindingRequest sbr) {
        boolean ret = true;
        if (role == RTCIceRole.CONTROLLED) {
            if (sbr.hasAttribute("ICE-CONTROLLED")) {
                //eeek. clash of roles.
                boolean big = !sbr.localAgentHasBiggerTieBreaker(this.tieBreaker, "ICE-CONTROLLED");
                if (big) {
                    ret = false;
                } else {
                    role = RTCIceRole.CONTROLLING;
                }
            }
        }
        if (role == RTCIceRole.CONTROLLING) {
            if (sbr.hasAttribute("ICE-CONTROLLING")) {
                //eeek. clash of roles.
                boolean big = sbr.localAgentHasBiggerTieBreaker(this.tieBreaker, "ICE-CONTROLLING");
                if (big) {
                    StunErrorTransaction err = new StunErrorTransaction(ice, sbr);
                    ret = false;
                } else {
                    role = RTCIceRole.CONTROLLED;
                }
            }
        }
        return ret;
    }

    /**
     * Received a stun packet that doesn't have a pre-existing transaction So we
     * potentially create a new transaction for it.
     *
     * @param p
     * @param prot
     * @param ipv
     * @return
     */
    public List<StunTransaction> receivedNew(StunPacket p, RTCIceProtocol prot, int ipv) {
        List<StunTransaction> ret = null;

        if (p instanceof StunBindingRequest) {
            // todo someone should check that the name/pass is right - who and how ?
            // check for required attributes.

            final StunBindingRequest sbr = (StunBindingRequest) p;

            if (sbr.hasRequiredIceAttributes()) {
                ret = new ArrayList();
                if (sbr.isUser(iceGatherer.getLocalParameters().usernameFragment)) {
                    RTCIceCandidatePair pair = findMatchingPair(sbr, prot, ipv);
                    if ((selectedPair != null) && (pair != selectedPair)) {
                        Log.verb("We have a selected pair - ignoring this..." + sbr);
                        return (ret);
                    }
                    if (pair == null) {
                        Log.verb("about to mkpair from " + sbr.toString() + " ipv" + ipv);
                        pair = mkPair(sbr, prot, ipv);
                        Log.verb("create pair " + ((pair == null) ? "Failed " : pair.toString()) + " ipv" + ipv);
                    }
                    StunTransaction replyTrans = null;
                    if (checkRoleOk(sbr)) {
                        replyTrans = new StunBindingTransaction(ice, sbr);
                        replyTrans.setCause("inbound");
                        if (pair != null) {
                            pair.recvdInbound(sbr, this, transMan);
                            Log.verb("adding " + replyTrans.toString() + " to do reply");
                        } else {
                            Log.verb("Not adding " + replyTrans.toString() + " to do reply");
                        }
                    } else {
                        replyTrans = new StunErrorTransaction(ice, sbr);
                    }
                    ret.add(replyTrans);
                } else {
                    Log.verb("Ignored bining transaction - wrong user");
                }
            }
        }
        return ret;
    }

    public RTCIceCandidatePair findMatchingPair(StunBindingRequest p, RTCIceProtocol prot, int ipv) {
        /*InetSocketAddress near = p.getNear();
        InetSocketAddress far = p.getFar();*/
        long pri = p.getPriority();
        RTCIceCandidate t_near = RTCIceCandidate.mkTempCandidate(p.getNear(), prot, ipv, pri);
        RTCIceCandidate t_far = RTCIceCandidate.mkTempCandidate(p.getFar(), prot, ipv, pri);
        Optional<RTCIceCandidatePair> cp = null;
        synchronized (candidatePairs) {
            cp = candidatePairs.stream().filter((RTCIceCandidatePair icp) -> {
                return icp.sameEnough(t_near, t_far);
            }).findAny();
        }
        return cp.isPresent() ? cp.get() : null;
    }

    private RTCIceCandidatePair mkPair(StunBindingRequest p, RTCIceProtocol prot, int ipv) {
        long pri = p.getPriority();
        DatagramChannel ch = p.getChannel();
        RTCLocalIceCandidate t_near = RTCLocalIceCandidate.mkTempCandidate(p.getNear(), prot, ipv, pri, ch);
        RTCIceCandidate t_far = RTCIceCandidate.mkTempCandidate(p.getFar(), prot, ipv, pri);
        Optional<RTCIceCandidate> fopt;
        synchronized (remoteCandidates) {
            fopt = this.remoteCandidates.stream().filter((RTCIceCandidate r) -> {
                return r.sameEnough(t_far);
            }).findAny();
        }
        RTCIceCandidate far = fopt.orElse(t_far);
        Optional<RTCLocalIceCandidate> nopt = this.getLocalCandidates().stream().filter((RTCLocalIceCandidate r) -> {
            return r.sameEnough(t_near);
        }).findAny();
        RTCLocalIceCandidate near = nopt.orElse(t_near);
        return addPair(near, far);
    }

    RTCIceRole getRole() {
        return this.role;
    }

    /**
     * @return the tieBreaker
     */
    public long getTieBreaker() {
        return tieBreaker;
    }

    private List<RTCLocalIceCandidate> getLocalCandidates() {
        return this.iceGatherer.getLocalCandidates();
    }

    public void triggerNominatablePair() {
        Optional<RTCIceCandidatePair> npair;
        if ((this.role == role.CONTROLLING)) {
            Log.debug("Looking for a nominateable pair  ");
            // lets try and nominate something....
            synchronized (candidatePairs) {
                npair = candidatePairs.stream().filter((RTCIceCandidatePair r) -> {
                    return (r.isNominateable());
                }).findFirst(); // strictly we should order this by priority and _find first_
            }
            if (npair.isPresent()) {
                RTCIceCandidatePair nomnom = npair.get();
                Log.debug("Triggering a nomination  on " + nomnom);
                StunTransaction tra = nomnom.triggerNomination(this);
                if (tra != null) {
                    this.transMan.addTransaction(tra);
                }
            }
        }
    }

    public RTCIceTransportState getState() {
        return this.state;
    }

    public void sendDtlsPkt(byte[] buf, int off, int len) throws IOException {
        Log.verb("Want to send dtls packet on " + (selectedPair == null ? "null" : selectedPair.toString()));
        if (selectedPair != null) {
            selectedPair.sendTo(buf, off, len);
        } else {
            Log.warn("Null selected pair, can't send non ice packet yet");
            throw new IOException("Null selected pair, can't send non ice packet yet");
        }
    }

    public void onError(RTCEventData e) {
        Log.warn("triggered Rtans error");
        if ((e != null) && (e instanceof StunAttribute.ErrorAttribute)) {
            StunAttribute.ErrorAttribute er = (StunAttribute.ErrorAttribute) e;
            if (er.code == 87) {
                if (this.role == role.CONTROLLING) {
                    Log.warn("Swapping roles....");
                    this.role = role.CONTROLLED;
                }
            }
        }
    }

    public int getMTU() {
        IceEngine ice = this.iceGatherer.getIceEngine();
        return ice.getMTU();
    }

    /* 
    public void pruneExcept(RTCIceCandidatePair sp) {
        if (sp != null) {
            transMan.pruneExcept(sp);
            // instead of pruning the candidate pair list - we just make a new one with one entry :-)
            ArrayList<RTCIceCandidatePair> nsp = new ArrayList<RTCIceCandidatePair>();
            nsp.add(sp);
            candidatePairs = nsp;
            Log.debug("pruned candidate Pairs to _just_ selected pair");
        }
    } 
     */
    public void listPairs() {

        StringBuffer list = new StringBuffer();
        synchronized (candidatePairs) {
            candidatePairs.stream().forEach((p) -> {
                list.append(p.toString()).append("\n");
            });
        }
        Log.debug(list.toString());
    }

    public boolean setSelected(RTCIceCandidatePair sel) {
        boolean ret = false;
        if (sel != selectedPair) {
            if ((selectedPair == null) || (sel == null)) {
                selectedPair = sel;
                if (selectedPair != null) {
                    Log.debug("selected candiate pair now " + selectedPair.toString());
                    ret = true;
                }
                if (oncandidatepairchange != null) {
                    Log.debug("candiate pair changed");
                    oncandidatepairchange.onEvent(sel);
                } else {
                    Log.warn("No one listening to candidate pair changes");
                }
            } else {
                Log.error("Trying to swap slelected pair ?!?");
            }
        } else {
            Log.warn("duplicate pair selection");
        }
        if (ret) {
            sel.futureConsentBindingTransaction(this, transMan);
        }
        return ret;
    }

    public void disconnectedSelected() {
        Log.debug("IceTransport disconnected ...because no packets received on " + selectedPair == null ? "" : selectedPair.toString());
        this.setState(RTCIceTransportState.DISCONNECTED);
    }

    private void pairChangedState(RTCIceCandidatePair p) {

        RTCIceCandidatePairState pstate = p.getState();
        switch (pstate) {
            case SUCCEEDED:
                if (this.state == RTCIceTransportState.CHECKING) {
                    this.setState(RTCIceTransportState.CONNECTED);
                    this.triggerNominatablePair();
                }
                break;
            case NOMINATED:
                if (this.state == RTCIceTransportState.CONNECTED) {
                    if (setSelected(p)) {
                        this.setState(RTCIceTransportState.COMPLETED);
                    }
                }
                break;
            case FAILED:
                if (p == selectedPair) {
                    this.setState(RTCIceTransportState.DISCONNECTED);
                    // strictly we could/should go looking for something else that works and nominate that.
                    // TODO...
                    this.setSelected(null);
                }
                break;
        }
    }

    public RTCIceCandidatePair findCandiatePair(DatagramChannel dgc, InetSocketAddress far) {
        Optional<RTCIceCandidatePair> cp;
        synchronized (candidatePairs) {
            cp = candidatePairs.stream().filter((RTCIceCandidatePair icp) -> {
                return icp.sameAsMe(dgc, far);
            }).findAny();
        }
        return cp.orElse(null);
    }
}
