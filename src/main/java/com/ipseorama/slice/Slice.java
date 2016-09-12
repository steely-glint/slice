/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

import com.ipseorama.slice.ORTC.RTCIceGatherer;
import com.ipseorama.slice.ORTC.RTCIceParameters;
import com.ipseorama.slice.ORTC.RTCIceTransport;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;

/**
 *
 * @author tim
 */
public class Slice {

    RTCIceTransport ice;

    public Slice() {

    }

    void accept() {
        RTCIceGatherer ig = new RTCIceGatherer();
        RTCIceComponent comp = RTCIceComponent.RTP; // lies...
        ice = new RTCIceTransport(ig,
                RTCIceRole.CONTROLLED,
                comp);
        String uf = "iceUser";
        String pass = "icepass";
        RTCIceParameters remoteParameters = new RTCIceParameters(uf, pass, false);
        ice.start(ig, remoteParameters, RTCIceRole.CONTROLLED);
    }

}
