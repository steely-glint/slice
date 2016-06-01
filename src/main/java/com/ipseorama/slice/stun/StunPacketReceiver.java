/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.phono.srtplight.Log;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tim
 */
public class StunPacketReceiver {

    /**
     * Passed an inbound (raw) Stun packet, it decodes it, processes it, and
     * adds any replies to the outbound Queue
     *
     * @param packet inbound raw packet - or null if the previous timeout
     * expired
     * @param outboundQueue outbound queue - packets are added here
     * @return when to call next
     *
     * The design goal here is _not_ to have any threads or timers underneath
     * here, we make the caller responsible for that - meaning that if they do
     * actors, blocking threads, nio or whatever, we don't care.
     */
    public long receivePacket(DatagramPacket packet, Queue<StunPacket> outboundQueue) {
        long tick = 1000;
        return tick;
    }

    public long addOutboundPacket(StunPacket s,TransactionEngine t){
        return 1000;
    }

    public static class TransactionEngine {

        public TransactionEngine() {
        }
    }

 
}
