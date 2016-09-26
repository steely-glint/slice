/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidatePairState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.ipseorama.slice.stun.IceStunBindingTransaction;
import com.ipseorama.slice.stun.StunBindingResponse;
import com.ipseorama.slice.stun.StunBindingTransaction;
import com.ipseorama.slice.stun.StunTransaction;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;

/**
 *
 * @author tim
 */
public class RTCIceCandidatePair implements RTCEventData{

    private final RTCIceCandidate local;
    private final RTCIceCandidate remote;
    private RTCIceCandidatePairState state;
    private boolean nominated;
    public EventHandler onDtls ;

    RTCIceCandidatePair(RTCIceCandidate local, RTCIceCandidate remote) {
        this.local = local;
        this.remote = remote;
        this.state = RTCIceCandidatePairState.WAITING;
    }

    /**
     * @return the local
     */
    public RTCIceCandidate getLocal() {
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
        state = newState;
    }

    public String toString() {
        return "CandidatePair is " + this.state.toString().toUpperCase() + "\n\tlocal :" + local.toString() + "\n\tremote :" + remote.toString();
    }

    boolean sameEnough(RTCIceCandidate t_near, RTCIceCandidate t_far) {
        return getLocal().sameEnough(t_near) && getRemote().sameEnough(t_far);
    }

    StunTransaction trigger(RTCIceTransport trans) {
        StunTransaction ret = createTransaction(trans,"outbound triggered");
        return ret;
    }
    public StunTransaction queued(RTCIceTransport trans) {
        StunTransaction ret = createTransaction(trans,"outbound queued");
        return ret;
    }
    StunTransaction createTransaction(RTCIceTransport trans,String cause) {
        String host = this.remote.getIp();
        int port = (int) this.remote.getPort();
        RTCIceRole role = trans.getRole();
        long reflexPri = priority(role);

        long tiebreaker = trans.getTieBreaker();
        String outboundUser = trans.getRemoteParameters().usernameFragment +":"+trans.getLocalParameters().usernameFragment;

        IceStunBindingTransaction ret = new IceStunBindingTransaction(host, port,
                (int) reflexPri,
                role,
                tiebreaker,
                outboundUser);
        ret.setCause(cause);
        return ret;
    }
        
    public void updateState(RTCEventData e) {        /*
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

Waiting:
A check has not been performed for this pair, and can be performed as soon as it is the highest-priority Waiting pair on the check list.
In-Progress:
A check has been sent for this pair, but the transaction is in progress.
Succeeded:
A check for this pair was already done and produced a successful result.
Failed:
A check for this pair was already done and failed, either never producing any response or producing an unrecoverable failure response.
Frozen:
A check for this pair hasn't been performed, and it can't yet be performed until some other check succeeds, allowing this pair to unfreeze and move into the Waiting state.

        */
        Log.debug("update pair state based on transaction completion "+e.toString());
        if (e instanceof RTCTimeoutEvent){
            this.setState(RTCIceCandidatePairState.FAILED);
        } else if (e instanceof StunBindingTransaction){
            StunBindingTransaction sbt = (StunBindingTransaction) e;
            StunBindingResponse sbr = sbt.getResponse();
            // todo depending on what this looks like we may want to nominate this one.
            if (sbr.hasAttribute("USE-CANDIADTE")){
                this.nominated = true;
            }
            this.setState(RTCIceCandidatePairState.SUCCEEDED);
        } 
    }
    public boolean isNominated(){
        return nominated;
    } 

    void setNominated(boolean b) {
        nominated = b;
    }

    public void pushDTLS(byte[] rec, InetSocketAddress near, InetSocketAddress far) {
        if (onDtls != null){
           //if (getLocal().sameSocketAddress(near) && getRemote().sameSocketAddress(far)){
               RTCDtlsPacket dat = new RTCDtlsPacket();
               dat.data = rec;
               onDtls.onEvent(dat);
           //} else {
           //    Log.debug("dtls packet doesn't match selected candidate "+ this.toString()+ " vs " +far +" -> "+ near );
           //}
        } else {
            Log.debug("dumping dtls packet - no place to push it.");
        }
    }


}
