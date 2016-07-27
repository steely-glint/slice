/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.RTCTimeoutEvent;
import static com.ipseorama.slice.stun.StunBindingTransaction.MAXTRIES;

/**
 *
 * @author tim
 */
public class IceStunBindingTransaction extends StunBindingTransaction{
    
    public IceStunBindingTransaction(String host, int port) {
        super(host, port);
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
}
