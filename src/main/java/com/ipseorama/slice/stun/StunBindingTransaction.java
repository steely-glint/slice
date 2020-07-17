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
public class StunBindingTransaction extends StunTransaction implements RTCEventData {

    protected InetSocketAddress _far;
    final static int TIMEOUT = 200; // a stun server that responds in > 1sec isn't of intrest.
    final static int MAXTRIES = 4;
    protected InetSocketAddress _ref;
    StunBindingRequest inbound = null;
    StunBindingResponse response;
    IceEngine ice;
    private DatagramChannel _channel;

    public StunBindingTransaction(IceEngine e, String host, int port) {
        super();
        _far = new InetSocketAddress(host, port);
        ice = e;
        dueTime = ice.nextAvailableTime();
    }

    public StunBindingTransaction(IceEngine e,StunBindingRequest sbreq) {
        super(sbreq);
        _far = sbreq.getFar();
        inbound = sbreq;
        ice = e;
        dueTime = System.currentTimeMillis(); // this is a reply - so _now_ is good.
        _channel = sbreq.getChannel();
    }

    public InetSocketAddress getFar() {
        return _far;
    }

    @Override
    public void receivedReply(StunPacket r) {
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
                dueTime = ice.nextAvailableTime() + (TIMEOUT * retries++);
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
        if (_channel == null){
            throw new NullPointerException("Channel is null "+this.toString());
        } else {
            if (bind != null) {
                bind.setChannel(_channel);
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

    public void setChannel(DatagramChannel chan) {
        _channel = chan;
    }
    
    public DatagramChannel getChannel(){
        return _channel;
    }

}
