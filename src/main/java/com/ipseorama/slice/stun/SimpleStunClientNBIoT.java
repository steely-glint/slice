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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.Timer;

/**
 *
 * @author tim
 */
public class SimpleStunClientNBIoT extends StunBindingTransaction {

    static String ttyname = "/dev/cu.usbserial-142";

    public static void main(String[] argv) {
        Log.setLevel(Log.ALL);
        SimpleStunClientNBIoT me = new SimpleStunClientNBIoT("192.67.4.150", 5683);
        try {
            me.query();
        } catch (Exception ex) {
            Log.debug(ex.getMessage());
            ex.printStackTrace();
        }
    }
    private boolean unanswered = true;
    private StunTransactionManager stm;
    private DataInputStream _itty;

    private DataOutputStream _otty;
    static IceEngine dummyEngine = new IceEngine(){
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
    public SimpleStunClientNBIoT(String host, int port) {
        super(dummyEngine,host, port);
        byte[] nid =
        {(byte)0xB7,(byte)0xBB,(byte)0xE8,(byte)0xF2,(byte)0xD8,(byte)0x5C,(byte)0x5C,(byte)0x83,(byte)0x04,(byte)0xB8,(byte)0x26,(byte)0x1A};
        this.id = nid;
// try {
            stm = new StunTransactionManager();
            stm.addTransaction(this);
/*
            File tty = new File(ttyname);
            if (!tty.exists() || !tty.canRead() || !tty.canWrite()) {
                throw new UnsupportedOperationException("can't open/read/write " + ttyname);
            }
            _itty = new DataInputStream(new FileInputStream(tty));
            _otty = new DataOutputStream(new FileOutputStream(tty));
            boolean happy = false;
            if (expect("AT+NRB", "OK")) {
                if (expect("AT+CGDCONT=1,\"IP\",\"NBIOT.Telekom\"\r\n", "OK")) {
                    if (expect("AT+COPS=1,2,\"12345\"\r\n", "OK")) {
                        if (expect("\"AT+NSOCR=DGRAM,17,16666,1\r\n", "OK")) {
                            happy = true;
                        }
                    }
                }
            
            }

        } catch (FileNotFoundException ex) {
            Log.error(ex.getMessage());
            ex.printStackTrace();
        }*/
    }

    public boolean expect(String message, String await) {
        boolean ret = false;

        try {
            Log.debug(" ---->" + message);
            _otty.writeBytes(message);
            _otty.flush();
            do {
                String line = _itty.readLine();
                Log.debug("<---- " + line);
                ret = (line.contains(await));
            } while (!ret);
        } catch (IOException ex) {
            Log.error(ex.getMessage());
            ex.printStackTrace();
        }
        return ret;

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
        // AT+NSOCR=DGRAM,17,1234,1 /* Create socket */ AT+NSOST=0,"177.25.7.15",1234,25,400241C7B17401724D0265703D3230313630383233 31363438
        String hextosend = StunPacket.hexString(send);
        int length = send.length;
        StringBuffer message = new StringBuffer();
        message.append("AT+NSOST=0,").append(_far.getAddress().getHostAddress()).append(",")
                .append(_far.getPort()).append(',')
                .append(length).append(',')
                .append(hextosend).append("\r\n");
        Log.debug("message -> " + message);
        byte[] dummy = {(byte)0x01,(byte)0x01,(byte)0x00,(byte)0x28,(byte)0x21,(byte)0x12,(byte)0xA4,(byte)0x42,
            (byte)0xB7,(byte)0xBB,(byte)0xE8,(byte)0xF2,(byte)0xD8,(byte)0x5C,(byte)0x5C,(byte)0x83,
            (byte)0x04,(byte)0xB8,(byte)0x26,(byte)0x1A,(byte)0x00,(byte)0x20,(byte)0x00,(byte)0x08,
            (byte)0x00,(byte)0x01,(byte)0x7D,(byte)0x15,(byte)0x76,(byte)0x9B,(byte)0xEC,(byte)0xE7,
            (byte)0x80,(byte)0x22,(byte)0x00,(byte)0x10,(byte)0x54,(byte)0x75,(byte)0x72,(byte)0x6E,
            (byte)0x53,(byte)0x65,(byte)0x72,(byte)0x76,(byte)0x65,(byte)0x72,(byte)0x20,(byte)0x30,
            (byte)0x2E,(byte)0x37,(byte)0x2E,(byte)0x33,(byte)0x80,(byte)0x28,(byte)0x00,(byte)0x04,
            (byte)0xC9,(byte)0xCA,(byte)0x6D,(byte)0x60};
        
        return dummy;
    }
}
