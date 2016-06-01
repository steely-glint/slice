/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import java.beans.EventHandler;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCIceTransport {

    final RTCIceGatherer iceGatherer;
    final RTCIceRole role;
    final RTCIceComponent component;
    protected RTCIceTransportState state;

    RTCIceTransportState getRTCIceTransportState() {
        return state;
    }

    List<RTCIceCandidate> getRemoteCandidates() {
        return null;
    }

    RTCIceCandidatePair getSelectedCandidatePair() {
        return null;
    }

    void start(RTCIceGatherer gatherer, RTCIceParameters remoteParameters, RTCIceRole role) {
    }

    void stop() {
    }

    RTCIceParameters getRemoteParameters() {
        return null;
    }

    RTCIceTransport createAssociatedTransport() {
        return null; // don't do this - rtcp-mux is good.
    }

    void addRemoteCandidate(RTCIceGatherCandidate remoteCandidate) {
    }

    void setRemoteCandidates(List<RTCIceCandidate> remoteCandidates) {
    }

    EventHandler onstatechange;
    EventHandler oncandidatepairchange;

    public RTCIceTransport(RTCIceGatherer ig,
            RTCIceRole r,
            RTCIceComponent comp) {
        this.iceGatherer = ig;
        this.role = r;
        this.component = comp;
    }

}
