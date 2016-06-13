/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceTransportState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
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

    public RTCIceTransportState getRTCIceTransportState() {
        return state;
    }

    public List<RTCIceCandidate> getRemoteCandidates() {
        return null;
    }

    public RTCIceCandidatePair getSelectedCandidatePair() {
        return null;
    }

    public void start(RTCIceGatherer gatherer, RTCIceParameters remoteParameters, RTCIceRole role) {
    }

    public void stop() {
    }

    public RTCIceParameters getRemoteParameters() {
        return null;
    }

    RTCIceTransport createAssociatedTransport() {
        return null; // don't do this - rtcp-mux is good.
    }

    public void addRemoteCandidate(RTCIceGatherCandidate remoteCandidate) {
    }

    public void setRemoteCandidates(List<RTCIceCandidate> remoteCandidates) {
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
