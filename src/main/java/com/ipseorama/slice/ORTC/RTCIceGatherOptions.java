/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceGatherPolicy;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCIceGatherOptions {

    private RTCIceGatherPolicy gatherPolicy;
    private List<RTCIceServer> iceServers;
    final private int _portMax;
    final private int _portMin; // avoid priv ports;

    public RTCIceGatherOptions() {
        this(1024, Character.MAX_VALUE);
    }

    public RTCIceGatherOptions(int portMin, int portMax) {
        _portMax = portMax;
        _portMin = portMin;
    }

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

    /**
     * @return the _portMax
     */
    public int getPortMax() {
        return _portMax;
    }

    /**
     * @return the _portMin
     */
    public int getPortMin() {
        return _portMin;
    }
    
}
