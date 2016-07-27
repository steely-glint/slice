/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidatePairState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;

/**
 *
 * @author tim
 */
class RTCIceCandidatePair {

    private final RTCIceCandidate local;
    private final RTCIceCandidate remote;
    private RTCIceCandidatePairState state;

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

    public long priority(RTCIceRole localRole){
        
        long g;
        long d;
        
        if (localRole == RTCIceRole.CONTROLLING){
            g = local.getPriority();
            d = remote.getPriority();
        } else {
            d = local.getPriority();
            g = remote.getPriority();
        }
        return (2L<<32)*Math.min(g,d) + 2*Math.max(g,d) + (g>d?1:0);
    }

    /**
     * @return the state
     */
    public RTCIceCandidatePairState getState() {
        return state;
    }
    public void setState(RTCIceCandidatePairState newState){
        state = newState;
    }
    public String toString(){
        return "CandidatePair is "+this.state.toString().toUpperCase() + "\n\tlocal :"+local.toString()+"\n\tremote :"+remote.toString();
    }



}
