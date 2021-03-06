/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCTimeoutEvent;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 *
 * @author thp
 */
public class StunErrorTransaction extends StunTransaction implements RTCEventData {

    protected InetSocketAddress _far;
    final static int TIMEOUT = 200; // a stun server that responds in > 1sec isn't of intrest.
    final static int MAXTRIES = 4;
    protected InetSocketAddress _ref;
    StunBindingRequest inbound = null;
    StunErrorResponse response;
    IceEngine ice;

    public StunErrorTransaction(IceEngine e, StunBindingRequest sbreq) {
        super(sbreq);
        _far = sbreq.getFar();
        inbound = sbreq;
        ice = e;
        dueTime = ice.nextAvailableTime();
    }

    public InetSocketAddress getFar() {
        return _far;
    }

    @Override
    public void receivedReply(StunPacket r) {
        Log.warn("unexpected packet  into StunError transaction " + r.getClass().getSimpleName());
    }

    @Override
    public StunPacket buildOutboundPacket() {

        StunErrorResponse rbind = new StunErrorResponse();
        rbind.setTid(this.getTid());
        rbind.setFar(_far);
        rbind.setRequiredAttributes(87,"Role conflict");
        complete = true;
        rbind.setChannel(inbound.getChannel());
        return rbind;
    }

    @Override
    public DatagramChannel getChannel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
