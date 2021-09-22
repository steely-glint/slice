/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidatePairState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.ipseorama.slice.stun.IceStunBindingTransaction;
import com.ipseorama.slice.stun.StunBindingRequest;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunTransaction;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 *
 * @author tim
 */
public class RTCIceCandidatePair implements RTCEventData {

    private static int COUNT = 0;

    private final RTCLocalIceCandidate local;
    private final RTCIceCandidate remote;
    private RTCIceCandidatePairState state;
    public EventHandler onDtls;
    public EventHandler onRTP;
    public EventHandler onRevoke;
    public EventHandler onStateChange;

    private final String name;
    private boolean checkedIn = false;
    private boolean checkedOut = false;
    private InetSocketAddress farIP;
    private final ArrayList<byte[]> packetStash;
    private SecureRandom rand;

    RTCIceCandidatePair(RTCLocalIceCandidate local, RTCIceCandidate remote) {
        this.local = local;
        this.remote = remote;
        this.rand = new SecureRandom();
        this.name = "RTCIceCandidatePair-" + (COUNT++);
        packetStash = new ArrayList();
    }

    /**
     * @return the local
     */
    public RTCLocalIceCandidate getLocal() {
        return local;
    }

    /**
     * @return the remote
     */
    public RTCIceCandidate getRemote() {
        return remote;
    }

    public long priority(RTCIceRole localRole) {

        long g;
        long d;

        if (localRole == RTCIceRole.CONTROLLING) {
            g = local.getPriority();
            d = remote.getPriority();
        } else {
            d = local.getPriority();
            g = remote.getPriority();
        }
        return (2L << 32) * Math.min(g, d) + 2 * Math.max(g, d) + (g > d ? 1 : 0);
    }

    /**
     * @return the state
     */
    public RTCIceCandidatePairState getState() {
        return state;
    }

    public void setState(RTCIceCandidatePairState newState) {
        if (this.state != newState) {
            Log.debug("CandidatePair " + name + " state " + state + " -> " + newState);
            this.state = newState;
            if (this.state == state.NOMINATED) {
                connectChannel();
            }
            if (this.onStateChange != null) {
                onStateChange.onEvent(newState);
            }
        } else {
            Log.debug("No change in CandidatePair " + name + " state " + state);
        }
    }

    public String toString() {
        return name + " (" + this.state.toString().toUpperCase() + " )\n\tlocal :" + local.toString() + "\n\tremote :" + remote.toString()
                + "\n\t dgc : " + this.local.getChannel();
    }

    boolean sameEnough(RTCIceCandidate t_near, RTCIceCandidate t_far) {
        return getLocal().sameEnoughIncludingWildCard(t_near) && getRemote().sameEnough(t_far);
    }

    StunTransaction trigger(RTCIceTransport trans) {
        StunTransaction ret = createAnOutBoundTransaction(trans, "outbound check triggered", false);
        return ret;
    }

    StunTransaction triggerNominated(RTCIceTransport trans) {
        // subtle thing here - even thogh we set Nominate to _true_ USE-CANDIDATE won't be set because we are 
        // CONTROLLED - meaning that the only impact is the the _reply_ to this transaction will NOMINATE this pair
        StunTransaction ret = createAnOutBoundTransaction(trans, "outbound check triggered by nomination", true);
        return ret;
    }

    StunTransaction triggerNomination(RTCIceTransport trans) {
        StunTransaction ret = createAnOutBoundTransaction(trans, "outbound nomination triggered", true);
        return ret;
    }

    public StunTransaction queued(RTCIceTransport trans) {
        StunTransaction ret = createAnOutBoundTransaction(trans, "outbound check queued", false);
        return ret;
    }

    void sentOutbound() {
        if (this.state == RTCIceCandidatePairState.WAITING) {
            this.setState(RTCIceCandidatePairState.INPROGRESS);
        }
    }

    StunTransaction createAnOutBoundTransaction(RTCIceTransport trans, String cause, boolean nominate) {
        if (this.state == RTCIceCandidatePairState.FROZEN) {
            state = RTCIceCandidatePairState.WAITING;
        }
        String host = this.remote.getIp();
        int port = (int) this.remote.getPort();
        RTCIceRole role = trans.getRole();
        long reflexPri = priority(role);
        IceEngine ice = trans.iceGatherer.getIceEngine();

        long tiebreaker = trans.getTieBreaker();
        final RTCIceCandidatePair pair = this;

        String outboundUser = trans.getRemoteParameters().usernameFragment + ":" + trans.getLocalParameters().usernameFragment;
        IceStunBindingTransaction ret = new IceStunBindingTransaction(ice, host, port,
                (int) reflexPri,
                role,
                tiebreaker,
                outboundUser, nominate) {

            @Override
            public StunPacket buildOutboundPacket() {
                pair.sentOutbound();
                return super.buildOutboundPacket(cause);
            }
        };
        ret.setCause(cause);
        ret.setPair(this);
        ret.setChannel(local.getChannel());
        ret.oncomplete = (RTCEventData data) -> {
            checkedOut = true;
            // well, this way works.
            farIP = ret.getFar();

            Log.debug("got a reply to " + ret);
            if (checkedIn) {
                // they have already sent us something...
                if (state == RTCIceCandidatePairState.INPROGRESS) {
                    this.setState(RTCIceCandidatePairState.SUCCEEDED);
                }
            }
            if (nominate) {
                Log.debug("nominating.... " + ret);
                this.setState(RTCIceCandidatePairState.NOMINATED);
            }
        };
        return ret;
    }

    /*
    2.3.  Nominating Candidate Pairs and Concluding ICE

   ICE assigns one of the ICE agents in the role of the controlling
   agent, and the other in the role of the controlled agent.  For each
   component of a data stream, the controlling agent nominates a valid
   pair (from the valid list) to be used for data.  The exact timing of
   the nomination is based on local policy.

   When nominating, the controlling agent lets the checks continue until
   at least one valid pair for each component of a data stream is found,
   and then it picks a valid pair and sends a STUN request on that pair,
   using an attribute to indicate to the controlled peer that it has
   been nominated.  This is shown in Figure 4.

             L                        R
             -                        -
             STUN request ->             \  L's
                       <- STUN response  /  check

                        <- STUN request  \  R's
             STUN response ->            /  check

             STUN request + attribute -> \  L's
                       <- STUN response  /  check

                           Figure 4: Nomination

   Once the controlled agent receives the STUN request with the
   attribute, it will check (unless the check has already been done) the
   same pair.  If the transactions above succeed, the agents will set
   the nominated flag for the pairs and will cancel any future checks
   for that component of the data stream.  Once an agent has set the
   nominated flag for each component of a data stream, the pairs become
   the selected pairs.  After that, only the selected pairs will be used
   for sending and receiving data associated with that data stream.
     */
 /*
        6.1.2.4.2.3. Updating Pair States

The agent sets the state of the pair that *generated* the check to Succeeded. Note that, the pair which *generated* the check may be different than the valid pair constructed in Section 6.1.2.4.2.2 as a consequence of the response. The success of this check might also cause the state of other checks to change as well. The agent MUST perform the following two steps:

The agent changes the states for all other Frozen pairs for the same media stream and same foundation to Waiting. Typically, but not always, these other pairs will have different component IDs.
If there is a pair in the valid list for every component of this media stream (where this is the actual number of components being used, in cases where the number of components signaled in the candidate exchange differs from initiating to responding agent), the success of this check may unfreeze checks for other media streams. Note that this step is followed not just the first time the valid list under consideration has a pair for every component, but every subsequent time a check succeeds and adds yet another pair to that valid list. The agent examines the check list for each other media stream in turn:
If the check list is active, the agent changes the state of all Frozen pairs in that check list whose foundation matches a pair in the valid list under consideration to Waiting.
If the check list is frozen, and there is at least one pair in the check list whose foundation matches a pair in the valid list under consideration, the state of all pairs in the check list whose foundation matches a pair in the valid list under consideration is set to Waiting. This will cause the check list to become active, and ordinary checks will begin for it, as described in Section 5.1.4.
If the check list is frozen, and there are no pairs in the check list whose foundation matches a pair in the valid list under consideration, the agent
groups together all of the pairs with the same foundation, and
for each group, sets the state of the pair with the lowest component ID to Waiting. If there is more than one such pair, the one with the highest-priority is used.
     */
 /*
Each candidate pair in the check list has a foundation and a state. The foundation is the combination of the foundations of the local and remote candidates in the pair. The state is assigned once the check list for each media stream has been computed. There are five potential values that the state can have:


     */
    public void pushDTLS(byte[] rec) {
        if (onDtls != null) {
            RTCDtlsPacket dat = new RTCDtlsPacket();
            dat.data = rec;
            onDtls.onEvent(dat);
        } else {
            Log.debug("dumping dtls packet - no place to push it.");
        }
    }

    public void pushRTP(DatagramPacket dgp) {
        if (onRTP != null) {
            RTCRtpPacket dat = new RTCRtpPacket();
            dat.data = dgp;
            onRTP.onEvent(dat);
            Log.verb("fired onEvent srtp packet ");
        } else {
            Log.debug("dumping rtp packet - no place to push it.");
        }
    }

    public void onRevoke() {
        if (onRevoke != null) {
            onRevoke.onEvent(null);
            Log.verb("fired onRevoke event ");
        } else {
            Log.debug("no one listening for onRevoke events");
        }
    }

    void recvdInbound(StunBindingRequest sbr, RTCIceTransport transp, StunTransactionManager m) {
        checkedIn = true;
        boolean farNominated = false;
        if (sbr.hasAttribute("USE-CANDIDATE")) {
            if (state == RTCIceCandidatePairState.SUCCEEDED) {
                setState(RTCIceCandidatePairState.NOMINATED);
            }
            farNominated = true;
        }
        if (!checkedOut) {
            if (state == RTCIceCandidatePairState.FROZEN) {
                setState(RTCIceCandidatePairState.INPROGRESS);
            }
            StunTransaction tr = farNominated ? triggerNominated(transp) : trigger(transp);
            if (tr != null) {
                m.addTransaction(tr);
            }
        } else if (state == RTCIceCandidatePairState.INPROGRESS) {
            setState(RTCIceCandidatePairState.SUCCEEDED);
        }
    }

    public boolean isNominateable() {
        return (state == RTCIceCandidatePairState.SUCCEEDED) && checkedIn && checkedOut;
    }

    public InetSocketAddress getFarIp() {
        return farIP;
    }

    public void sendTo(byte[] buf, int off, int len) throws IOException {
        if (state == RTCIceCandidatePairState.NOMINATED) {
            DatagramChannel ch = getLocal().getChannel();
            ByteBuffer src = ByteBuffer.wrap(buf, off, len);
            try {
                ch.write(src);
            } catch (IOException x) {
                Log.warn("Error sending to " + name);
                throw x;
            }
        } else {
            Log.warn("cant send : " + name + " not selected.");
        }
    }

    public void pushDTLSStash() {
        Log.debug("Empty DTLS packet stash of " + packetStash.size() + " on " + this.toString());
        packetStash.forEach((byte[] pkt) -> {
            this.pushDTLS(pkt);
        });
        packetStash.clear();
    }

    public void stashPacket(byte[] rec) {
        packetStash.add(rec);
        Log.debug("Added to DTLS packet stash of " + packetStash.size() + " on " + this.toString());
    }

    public boolean sameAsMe(DatagramChannel c, InetSocketAddress f) {
        /*String err = "";
        if (c == null) {
            err += " C is null.";
        }
        if (f == null) {
            err += " F is null.";
        }
        if (farIP == null) {
            err += " FarIP is null.";
        }
        if (local == null) {
            err += " Local is null. ";
        } else {
            DatagramChannel chan = local.getChannel();
            if (chan == null) {
                err += " Local.Chan is null";
            }
        }
        if (err.length() > 0){
            Log.warn(err);
        }*/
        if ((local == null) || (remote == null)) {
            Log.warn(name + " pair has < 2 candidates ");
        }
        DatagramChannel chan = this.getLocal().getChannel();
        if (chan != c) {
            Log.debug(name + " channel doesn't match");
        }
        boolean samesock = this.remote.sameSocketAddress(f);
        if (!samesock) {
            Log.debug(name + " far " + f + " != " + remote.getIp() + ":" + remote.getPort());
        }
        return ((remote != null)
                && (local != null)
                && (c == this.getLocal().getChannel())
                && this.remote.sameSocketAddress(f));
    }

    private void connectChannel() {
        //Log.warn("NOT connecting channel to "+farIP+". Channel will use sendTo ");
        try {
            DatagramChannel ch = this.local.getChannel();
            if (ch != null) {
                if (!ch.isConnected()) {
                    ch.connect(farIP);
                } else {
                    SocketAddress ofip = ch.getRemoteAddress();
                    if (!ofip.equals(farIP)) {
                        Log.warn("cant change connected address on" + this.name);
                    } else {
                        Log.warn("already connected " + this.name);
                    }
                }
            } else {
                Log.warn("No channel to connect on" + this.name);
            }
        } catch (IOException ex) {
            Log.error("Can't connect " + this.toString());
        }
    }

    void futureConsentBindingTransaction(RTCIceTransport trans, StunTransactionManager transMan) {
        if (this.state == state.NOMINATED) {
            if (!transMan.pairHasActiveTrans(this)) {
                StunTransaction ret = createAnOutBoundTransaction(trans, "outbound consent queued", false);
                int delay = 1000 + rand.nextInt(1000);
                ret.addDelay(delay);
                Log.debug("Queued consent req for " + ret.getDueTime() + " on " + this.name);
                RTCIceCandidatePair pair = this;
                ret.oncomplete = (RTCEventData data) -> {
                    Log.debug("Got consent reply, queue another on " + pair.name);
                    futureConsentBindingTransaction(trans, transMan);
                };
                ret.onerror = (RTCEventData data) -> {
                    Log.warn("Got consent revoked on " + pair.toString());
                    pair.setState(state.FAILED);
                };
                transMan.addTransaction(ret);
            } else {
                Log.debug(name + " already has active transactions ");
            }
            transMan.removeComplete();
        }

    }
}
