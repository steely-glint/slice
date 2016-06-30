/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.EventHandler;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 *
 * @author thp
 */
public abstract class StunTransaction {

    byte[] id;
    long dueTime;
    int retries;
    boolean complete;
    SecureRandom r = new SecureRandom();
    
    /**
     * used to signal that the transaction is complete
     * either due to timeout or a reply
     */
    public EventHandler oncomplete;

    /**
     * constructor for initiating transactions 
     */
    public StunTransaction() {
        id = new byte[12];
        r.nextBytes(id);
    }

    /**
     * constructor for transactions intitated by the far side.
     * @param pk 
     */
    public StunTransaction(StunPacket pk) {
        
    }
    
    /**
     * true when a satisfactory reply has happened.
     * @return 
     */
    boolean isComplete(){
        return complete;
    }
    
    public abstract void receive(StunPacket r);

    public abstract StunPacket buildOutboundPacket();

    public byte[] getTid(){
        return id;
    }
    /**
     * This is the time we _next_ expect something to happen.
     * Which may be either a transmit or a timeout.
     * @return 
     */
    public long getDueTime(){
        return dueTime;
    }
}
