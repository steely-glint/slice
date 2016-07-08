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

    private InetSocketAddress _far;
    final static int TIMEOUT = 200; // a stun server that responds in > 1sec isn't of intrest.
    final static int MAXTRIES = 4;
    private InetSocketAddress _ref;

    public StunBindingTransaction(String host, int port) {
        _far = new InetSocketAddress(host, port);
        dueTime = System.currentTimeMillis();
    }

    @Override
    public void received(StunPacket r) {
        if (r instanceof StunBindingResponse) {
            StunBindingResponse resp = (StunBindingResponse) r;
            _ref = resp.getReflex();
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
        return bind;
    }

    /**
     * @return the _ref
     */
    public InetSocketAddress getReflex() {
        return _ref;
    }

    
}
