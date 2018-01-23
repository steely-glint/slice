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
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.stun.StunBindingRequest;
import com.ipseorama.slice.stun.StunBindingTransaction;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunTransaction;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
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
    private InetSocketAddress dtlsTo;

    public RTCIceTransportState getRTCIceTransportState() {
        return state;
    }

    public List<RTCIceCandidate> getRemoteCandidates() {
        return remoteCandidates;
    }

    public RTCIceCandidatePair getSelectedCandidatePair() {
        return selectedPair;
    }

    public void start(RTCIceGatherer gatherer, RTCIceParameters remoteParameters, RTCIceRole role) {
        // for the moment we will ignore the new values and assume that the constructor was right....
        // and ignore the re-start semantics.
        IceEngine ice = gatherer.getIceEngine();

        this.transMan = gatherer.getStunTransactionManager();
        transMan.setTransport(this);
        final EventHandler oldAct = gatherer.onlocalcandidate;
        if (remoteParameters != null) {
            this.remoteParameters = remoteParameters;
        }
        this.role = role;
        gatherer.onlocalcandidate = (RTCEventData c) -> {
            if (c instanceof RTCIceCandidate) {
                RTCIceCandidate l = (RTCIceCandidate) c;
                List<RTCIceCandidate> remotes = new ArrayList(remoteCandidates);
                for (RTCIceCandidate r : remotes) {
                    addPair(l, r);
                }
            }
            if (oldAct != null) {
                oldAct.onEvent(c);
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
            this.remoteCandidates.add(r);
            List<RTCIceCandidate> locals = new ArrayList(iceGatherer.getLocalCandidates());
            for (RTCIceCandidate l : locals) {
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
                ret = 1;
            } else if (lcp < 0) {
                ret = -1;
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

    private void addPair(RTCIceCandidate l, RTCIceCandidate r) {
        if (l.getProtocol() == r.getProtocol() && l.getIpVersion() == r.getIpVersion()) {
            synchronized (candidatePairs) {
                boolean present = candidatePairs.stream().anyMatch((RTCIceCandidatePair p) -> {
                    return p.getLocal().equals(l) && p.getRemote().equals(r);
                });
                if (!present) {
                    RTCIceCandidatePair p = new RTCIceCandidatePair(l, r);
                    if (p != null) {
                        candidatePairs.add(p);
                        Log.debug("added candidate pair " + p.toString());
                        transMan.maybeAddTransactionForPair(p);
                    }
                } else {
                    Log.debug("ignoring exisiting candidate pair " + r.toString() + " " + l.toString());
                }
            }
        } else {
            Log.debug("ignoring incompatiple candidate pair " + r.toString() + " " + l.toString());
        }
    }

    public RTCIceCandidatePair nextCheck() {
        Optional<RTCIceCandidatePair> ret = candidatePairs.stream()
                .sorted(ordering)
                .limit(MAXCHECKS)
                .filter((RTCIceCandidatePair icp) -> {
                    return icp.getState() == RTCIceCandidatePairState.WAITING;
                })
                .findFirst();
        return ret.isPresent() ? ret.get() : null;
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
    public List<StunTransaction> received(StunPacket p, RTCIceProtocol prot, int ipv) {
        List<StunTransaction> ret = null;
        if (p instanceof StunBindingRequest) {
            // todo someone should check that the name/pass is right - who and how ?
            // check for required attributes.

            final StunBindingRequest sbr = (StunBindingRequest) p;

            if (sbr.hasRequiredIceAttributes()) {
                ret = new ArrayList();
                if (sbr.isUser(iceGatherer.getLocalParameters().usernameFragment)) {
                    RTCIceCandidatePair inbound = findMatchingPair(sbr, prot, ipv);
                    if (inbound == null) {
                        Log.verb("about to mkpair from " + sbr.toString() + " ipv" + ipv);
                        inbound = mkPair(sbr, prot, ipv);
                        Log.verb("create pair " + inbound.toString() + " ipv" + ipv);
                    }
                    if (role == RTCIceRole.CONTROLLED) {
                        inbound.setNominated(p.hasAttribute("USE-CANDIDATE"));
                    }
                    //to do: some state update on the pair here.
                    StunBindingTransaction replyTrans = new StunBindingTransaction(sbr);
                    replyTrans.setCause("inbound");
                    Log.verb("adding " + replyTrans.toString() + " to do reply");
                    ret.add(replyTrans);
                    if (inbound.getState() != RTCIceCandidatePairState.SUCCEEDED) {
                        // questionable state - so trigger a reverse check for this one.
                        StunTransaction triggeredTrans = inbound.trigger(this);
                        Log.verb("adding new Rtans " + triggeredTrans.toString() + " for trigger");
                        ret.add(triggeredTrans);
                        final RTCIceCandidatePair mypair = inbound;
                        mypair.setState(RTCIceCandidatePairState.INPROGRESS);
                        triggeredTrans.oncomplete = (RTCEventData e) -> {
                            Log.verb("triggered Rtans check complete. do something here....");
                            //to do: some state update on the pair here.
                            mypair.updateState(e);
                        };
                    } else {
                        Log.verb("Candidate Pair already SUCCEEDED no need to trigger -" + inbound);
                    }
                } else {
                    Log.verb("Ignored bining transaction - wrong user");
                }
            }
        }
        return ret;
    }

    private RTCIceCandidatePair findMatchingPair(StunBindingRequest p, RTCIceProtocol prot, int ipv) {
        /*InetSocketAddress near = p.getNear();
        InetSocketAddress far = p.getFar();*/
        long pri = p.getPriority();
        RTCIceCandidate t_near = RTCIceCandidate.mkTempCandidate(p.getNear(), prot, ipv, pri);
        RTCIceCandidate t_far = RTCIceCandidate.mkTempCandidate(p.getFar(), prot, ipv, pri);

        Optional<RTCIceCandidatePair> cp = this.candidatePairs.stream().filter((RTCIceCandidatePair icp) -> {
            return icp.sameEnough(t_near, t_far);
        }).findAny();
        return cp.isPresent() ? cp.get() : null;
    }

    private RTCIceCandidatePair mkPair(StunBindingRequest p, RTCIceProtocol prot, int ipv) {
        long pri = p.getPriority();
        RTCIceCandidate t_near = RTCIceCandidate.mkTempCandidate(p.getNear(), prot, ipv, pri);
        RTCIceCandidate t_far = RTCIceCandidate.mkTempCandidate(p.getFar(), prot, ipv, pri);
        Optional<RTCIceCandidate> fopt = this.remoteCandidates.stream().filter((RTCIceCandidate r) -> {
            return r.sameEnough(t_far);
        }).findAny();
        RTCIceCandidate far = fopt.orElse(t_far);
        Optional<RTCIceCandidate> nopt = this.getLocalCandidates().stream().filter((RTCIceCandidate r) -> {
            return r.sameEnough(t_near);
        }).findAny();
        RTCIceCandidate near = nopt.orElse(t_near);
        RTCIceCandidatePair ret = new RTCIceCandidatePair(near, far);
        candidatePairs.add(ret);
        return ret;
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

    private List<RTCIceCandidate> getLocalCandidates() {
        return this.iceGatherer.getLocalCandidates();
    }

    public RTCIceCandidatePair findValidNominatedPair() {
        Optional<RTCIceCandidatePair> npair = this.candidatePairs.stream().filter((RTCIceCandidatePair r) -> {
            return r.isNominated() && r.getState() == RTCIceCandidatePairState.SUCCEEDED;
        }).findAny(); // strictly we should order this by priority and _find first_
        RTCIceCandidatePair ret = npair.isPresent() ? npair.get() : null;
        //Log.verb("selected pair          "+ret);
        //Log.verb("old selected pair was  "+selectedPair);
        if (ret != selectedPair) {
            Log.debug("have new selected pair "+ret);
            Log.debug("old selected pair was  "+selectedPair);

            if (selectedPair == null) {
                this.setState(RTCIceTransportState.CONNECTED);
            }
            if (ret == null){
                this.setState(RTCIceTransportState.DISCONNECTED);
            }
            selectedPair = ret;
            dtlsTo = new InetSocketAddress(selectedPair.getRemote().getIp(),selectedPair.getRemote().getPort());
            if (null != this.oncandidatepairchange){
                 oncandidatepairchange.onEvent(selectedPair);
            }
        }
        return ret;
    }

    public RTCIceTransportState getState() {
        return this.state;
    }

    public void sendDtlsPkt(byte[] buf, int off, int len) {
        Log.debug("Will send dtls packet to "+dtlsTo);
        IceEngine ice = this.iceGatherer.getIceEngine();
        
        
        ice.sendTo(buf,off,len,dtlsTo);
        // use selectedPair  to send packet.
    }




}
