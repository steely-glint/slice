/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCTimeoutEvent;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.SecureRandom;
import java.util.Set;

/**
 *
 * @author tim
 */
public class SimpleStunClientNio extends StunBindingTransaction {

    public static void main(String[] argv) {
        Log.setLevel(Log.ALL);
        SimpleStunClientNio me = new SimpleStunClientNio("146.148.121.175", 3478);
        try {
            me.query();
        } catch (Exception ex) {
            Log.debug(ex.getMessage());
            ex.printStackTrace();
        }
    }
    private boolean unanswered = true;
    private StunTransactionManager stm;
    static IceEngine dummyEngine = new IceEngine() {
        @Override
        public void start(DatagramSocket ds, StunTransactionManager tm) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void start(Selector ds, StunTransactionManager tm) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isStarted() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addIceCreds(String user, String pass) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StunTransactionManager getTransactionManager() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }


        @Override
        public int getMTU() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long nextAvailableTime() {
            return System.currentTimeMillis();
        }

    };
    Selector s_sell;
    Selector r_sell;

    DatagramChannel dgc;

    public SimpleStunClientNio(String host, int port) {
        super(dummyEngine, host, port);
        try {
            r_sell = Selector.open();
            s_sell = Selector.open();

            dgc = createDatagramChannel("192.168.0.100");
            dgc.register(r_sell, SelectionKey.OP_READ);
            //dgc.register(s_sell, SelectionKey.OP_WRITE);
            stm = new StunTransactionManager();
            stm.addTransaction(this);
        } catch (Exception x) {
            Log.debug("problem " + x.getMessage());
            x.printStackTrace();
        }
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

    private DatagramChannel createDatagramChannel(String home) throws IOException {
        DatagramChannel ret = DatagramChannel.open();

        int portMin = 9000;
        int portMax = 10000;
        SecureRandom rand = new SecureRandom();
        int rangeSz = portMax - portMin;
        for (int tries = 0; tries < rangeSz * 2; tries++) {
            int pno = portMin + rand.nextInt(rangeSz);
            try {
                InetSocketAddress local = new InetSocketAddress(home, pno);
                Log.verb("new local socket address " + local.toString());
                ret.bind(local);
                ret.configureBlocking(false);
                ret.socket().setTrafficClass(46);
                break;
            } catch (SocketException ex) {
                Log.debug("retry with new port no " + pno + " is in use on " + home);
            }
        }
        if (ret == null) {
            throw new IOException("No free ports");
        }
        return ret;
    }

    byte[] sendAndRecv(byte[] send, int nap) throws Exception {
        byte[] rcvd = null;
        ByteBuffer rec = ByteBuffer.allocate(1024);
        ByteBuffer src = ByteBuffer.wrap(send);
        boolean sent = false;
       try {
/*            int bitm = s_sell.select(nap);
            if (bitm != 0) {
                Log.debug("ok to send " + bitm);
                Set<SelectionKey> keys = s_sell.selectedKeys();
                for (SelectionKey k : keys) {
                    if (k.isWritable() && !sent) {
                        Log.debug("about to send ");
                        DatagramChannel rdc = (DatagramChannel) k.channel();
*/
    
                        dgc.send(src, this._far);
                        sent = true;
/*                    }
                }
            }*/
            Log.debug("starting to listen sent=" + sent);
            int bitm = r_sell.select(nap);
            if (bitm != 0) {
                Log.debug("ok to recv " + bitm);
                Set<SelectionKey> keys = r_sell.selectedKeys();
                for (SelectionKey k : keys) {
                    if (k.isReadable()) {
                        Log.debug("about to rec ");
                        DatagramChannel rdc = (DatagramChannel) k.channel();
                        SocketAddress from = rdc.receive(rec);
                        Log.debug("rec.from is "+from);
                        rec.flip();
                        rcvd = new byte[rec.remaining()];
                        rec.get(rcvd);
                        Log.debug("returning "+rcvd.length);

                    }

                }
            }
        } catch (SocketTimeoutException tx) {
            Log.debug("retry...");
        }
        return rcvd;
    }
}
