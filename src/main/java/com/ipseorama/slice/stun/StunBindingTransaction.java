/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCTimeoutEvent;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;

/**
 *
 * @author thp
 */
public class StunBindingTransaction extends StunTransaction implements RTCEventData {

    protected InetSocketAddress _far;
    final static int TIMEOUT = 200; // a stun server that responds in > 1sec isn't of intrest.
    final static int MAXTRIES = 4;
    protected InetSocketAddress _ref;
    StunBindingRequest inbound = null;
    StunBindingResponse response;

    public StunBindingTransaction(String host, int port) {
        super();
        _far = new InetSocketAddress(host, port);
        dueTime = System.currentTimeMillis();
    }

    public StunBindingTransaction(StunBindingRequest sbreq) {
        super(sbreq);
        _far = sbreq.getFar();
        inbound = sbreq;
        dueTime = System.currentTimeMillis() - 10;
    }

    public InetSocketAddress getFar() {
        return _far;
    }

    @Override
    public void received(StunPacket r) {
        if (r instanceof StunBindingResponse) {
            response = (StunBindingResponse) r;
            _ref = response.getReflex();
            complete = true;
            if (oncomplete != null) {
                oncomplete.onEvent(this);
            }
        } else {
            Log.warn("unexpected packet type into StunBinding transaction " + r.getClass().getSimpleName());
        }
    }

    @Override
    public StunPacket buildOutboundPacket() {
        StunPacket bind = null;
        if (inbound == null) { // near is the requestor...
            if (retries > MAXTRIES) {
                RTCTimeoutEvent e = new RTCTimeoutEvent();
                complete = true;
                if (oncomplete != null) {
                    oncomplete.onEvent(e);
                }
            } else {
                bind = new StunBindingRequest();
                bind.setTid(this.getTid());
                bind.setFar(_far);
                dueTime = System.currentTimeMillis() + (TIMEOUT * retries++);
            }
        } else {
            // someone sent us a request.
            StunBindingResponse rbind = new StunBindingResponse();
            rbind.setTid(this.getTid());
            rbind.setFar(_far);
            String ufrags = inbound.getUserName();
            rbind.setRequiredAttributes(_far, ufrags);
            bind = rbind;
            complete = true; // oneshot. but no one cares...
            if (oncomplete != null) {
                oncomplete.onEvent(null);
            }
        }
        return bind;
    }

    /**
     * @return the _ref
     */
    public InetSocketAddress getReflex() {
        return _ref;
    }

    public StunBindingResponse getResponse() {
        return response;
    }

}
