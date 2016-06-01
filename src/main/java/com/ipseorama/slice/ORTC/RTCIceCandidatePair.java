/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

/**
 *
 * @author tim
 */
class RTCIceCandidatePair {

    private final RTCIceCandidate local;
    private final RTCIceCandidate remote;

    RTCIceCandidatePair(RTCIceCandidate local, RTCIceCandidate remote) {
        this.local = local;
        this.remote = remote;
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
}
