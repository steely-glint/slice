/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import java.util.List;

/**
 *
 * @author tim
 */
class RTCIceGatherOptions {

    private RTCIceGatherPolicy gatherPolicy;
    private List<RTCIceServer> iceServers;

    /**
     * @return the gatherPolicy
     */
    public RTCIceGatherPolicy getGatherPolicy() {
        return gatherPolicy;
    }

    /**
     * @param gatherPolicy the gatherPolicy to set
     */
    public void setGatherPolicy(RTCIceGatherPolicy gatherPolicy) {
        this.gatherPolicy = gatherPolicy;
    }

    /**
     * @return the iceServers
     */
    public List<RTCIceServer> getIceServers() {
        return iceServers;
    }

    /**
     * @param iceServers the iceServers to set
     */
    public void setIceServers(List<RTCIceServer> iceServers) {
        this.iceServers = iceServers;
    }
}
