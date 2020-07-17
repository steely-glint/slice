/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.EventHandler;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 *
 * @author thp
 */
public abstract class StunTransaction {

    byte[] id;
    long dueTime;
    int retries;
    boolean complete;
    String cause = "unknown";
    SecureRandom r = new SecureRandom();
    
    /**
     * used to signal that the transaction is complete
     * either due to timeout or a reply
     */
    public EventHandler oncomplete;
    public EventHandler onerror;


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
        id = pk.getTid();
    }
    Integer getTidHash(){
        return Arrays.hashCode(id);
    }
    /**
     * true when a satisfactory reply has happened.
     * @return 
     */
    boolean isComplete(){
        return complete;
    }
    /**
     * when we get a stun packet who's transaction ID matches this object,
     * pass it here for processing
     * @param r 
     */
    public abstract void receivedReply(StunPacket r);

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
    public void setCause(String c){
        cause = c;
    }
    public String toString(){
        return "{"+cause+"} stun Transaction type "+this.getClass().getSimpleName()+" complete="+this.isComplete()+ "tid ="+ StunPacket.hexString(id);
    }

    public abstract DatagramChannel getChannel();

}
