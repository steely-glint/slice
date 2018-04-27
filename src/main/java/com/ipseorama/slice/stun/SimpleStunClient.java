/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCTimeoutEvent;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.phono.srtplight.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

/**
 *
 * @author tim
 */
public class SimpleStunClient extends StunBindingTransaction {

    public static void main(String[] argv) {
        Log.setLevel(Log.ALL);
        SimpleStunClient me = new SimpleStunClient("146.148.121.175", 3478);
        try {
            me.query();
        } catch (Exception ex) {
            Log.debug(ex.getMessage());
            ex.printStackTrace();
        }
    }
    private boolean unanswered = true;
    private StunTransactionManager stm;

    public SimpleStunClient(String host, int port) {
        super(host, port);
        stm = new StunTransactionManager();
        stm.addTransaction(this);
    }

    private void query() throws Exception {
        this.oncomplete = (RTCEventData e) -> {
            Log.debug("got binding reply - or timeout");
            unanswered = false;
            if (e instanceof RTCTimeoutEvent) {
                Log.debug("got binding timeout");
            }
            if (e instanceof StunBindingTransaction) {
                StunBindingTransaction st = (StunBindingTransaction) e;
                InetSocketAddress ref = st.getReflex();
                Log.info("Result is " + ref.toString());
            }
        };
        while (unanswered) {
            StunPacket pkt = this.buildOutboundPacket();
            byte[] outb = pkt.outboundBytes();

            int nap = (int) (this.dueTime - System.currentTimeMillis());
            if (nap < 10) {
                nap = 10;
            }
            byte[] inb = sendAndRecv(outb, nap);
            if (inb != null) {
                StunPacket recv = StunPacket.mkStunPacket(inb, null, null, stm);
                stm.receivedPacket(recv, RTCIceProtocol.UDP, 4);
            }
        }

    }

    byte[] sendAndRecv(byte[] send, int nap) throws Exception {
        byte[] rcvd = null;
        DatagramSocket dgs = new DatagramSocket();

        DatagramPacket spkt = new DatagramPacket(send, send.length, this._far);
        DatagramPacket rpkt = new DatagramPacket(new byte[1024], 0, 1024);
        dgs.send(spkt);
        dgs.setSoTimeout(nap);
        try {
            dgs.receive(rpkt);
            rcvd = new byte[rpkt.getLength()];
            System.arraycopy(rpkt.getData(), rpkt.getOffset(), rcvd, 0, rpkt.getLength());
        } catch (SocketTimeoutException tx) {
            Log.debug("retry...");
        }
        return rcvd;
    }
}
