/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

import com.ipseorama.slice.ORTC.RTCIceCandidatePair;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thp
 *
 * Simple experimental implementation of Ice engine
 *
 */
public class ThreadedIceEngine implements IceEngine {

    StunTransactionManager _trans;
    private DatagramSocket _sock;
    private Thread _send;
    private Thread _rcv;
    private boolean _started = false;
    static int POLL = 1000;
    static int MAXSILENCE = 40;
    static int Ta = 5;
    private int mtu = StunPacket.MTU;
    private Map<String, String> miPass = new HashMap();
    private RTCIceCandidatePair selected;
    long nextAvailableTime;

    public synchronized void start(DatagramSocket ds, StunTransactionManager tm) {
        if (_started) {
            throw new java.lang.IllegalStateException("Can't start a Threaded Ice engine more than once.");
        }
        nextAvailableTime = System.currentTimeMillis();
        _sock = ds;
        int port = ds.getLocalPort();
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(ds.getLocalAddress());
            if (ni != null) {
                int m = ni.getMTU();

                Log.debug(" MTU is " + m);
                if ((m > 100) && (m < 2000)) {
                    mtu = m;
                }
            }
        } catch (SocketException ex) {
            Log.debug("Can't figure out MTU");
        }
        _trans = tm;
        if ((_sock == null) || (_trans == null)) {
            throw new java.lang.IllegalArgumentException("Need non-null socket and transaction manager to start");
        }

        Runnable ior = () -> {

            rcvloop();
        };
        _rcv = new Thread(ior, "ice-rcvr(" + port + ") --<<--");
        _rcv.setPriority(Thread.MAX_PRIORITY);
        _rcv.start();
        Runnable ios = () -> {
            sendloop();
        };
        _send = new Thread(ios, "ice-send(" + port + ") -->>--");
        _send.start();
        _started = true;

    }

    @Override
    public long nextAvailableTime() {
        long now = System.currentTimeMillis();
        if (nextAvailableTime < now) {
            nextAvailableTime = now;
        }
        long ret = nextAvailableTime;
        nextAvailableTime += Ta;
        return ret;
    }

    @Override
    public void addIceCreds(String user, String pass) {
        miPass.put(user, pass);
        if (Log.getLevel() >= Log.DEBUG) {
            Log.debug("mipass list is:");
            miPass.forEach((String u, String p) -> {
                Log.debug("\t" + u + " " + p);
            });
        }
    }

    /*
    void tick() {
        long now = System.currentTimeMillis();
        List<StunPacket> rs = _trans.transact(now);
        for (StunPacket r : rs) {
            try {
                byte[] p = r.outboundBytes();
                SocketAddress far = r.getFar();
                DatagramPacket d = new DatagramPacket(p, 0, p.length, far);
                _sock.send(d);
            } catch (Exception ex) {
                Log.warn("Failed to build outbound packet");
            }
        }

    }
     */
    private void rcvloop() {
        try {
            byte[] recbuf = new byte[mtu];
            int timeoutCount = 0;
            _sock.setSoTimeout(POLL);
            InetSocketAddress near = (InetSocketAddress) _sock.getLocalSocketAddress();
            while (_rcv != null) {
                try {
                    DatagramPacket dgp = new DatagramPacket(recbuf, 0, recbuf.length);
                    try {
                        _sock.receive(dgp);
                        InetSocketAddress far = (InetSocketAddress) dgp.getSocketAddress();
                        int ipv = far.getAddress() instanceof java.net.Inet4Address ? 4 : 6;

                        int len = dgp.getLength();
                        byte rec[] = new byte[len];
                        System.arraycopy(recbuf, 0, rec, 0, len);
                        // switch on first byte here - stun/dtls/rtp ?
                        /*
                        
                   +----------------+
                   | 127 < B < 192 -+--> forward to RTP
                   |                |
       packet -->  |  19 < B < 64  -+--> forward to DTLS
                   |                |
                   |       B < 2   -+--> forward to STUN
                   +----------------+

                         */
                        byte b = rec[0];
                        if ((b < 2) && (b >= 0)) {
                            StunPacket rp = StunPacket.mkStunPacket(rec, miPass, near, _trans);
                            rp.setFar(far);
                            Log.verb(StunPacket.hexString(rp.getTid()) + "got packet type " + rp.getClass().getSimpleName() + " from " + far);
                            _trans.receivedPacket(rp, RTCIceProtocol.UDP, ipv);
                            // tell our friend that we may have some work for them.
                            synchronized (_trans) {
                                _trans.notifyAll();
                            }
                        } else if ((19 < b) && (b < 64)) {
                            Log.debug("push inbound DTLS packet");
                            if (selected != null) {
                                selected.pushDTLS(rec, near, far);
                            } else {
                                // strictly this is wrong - we should stack them...
                                Log.debug("dumping DTLS packet - no selected pair - yet...");
                            }
                        } else if (b < 0) {
                            if (selected != null) {
                                selected.pushRTP(dgp);
                            } else {
                                // strictly this is wrong - we should stack them...
                                Log.debug("dumping RTP packet - no selected pair - yet...");
                            }
                        } else {
                            Log.verb("packet first byte " + b);
                        }
                        timeoutCount = 0;
                    } catch (SocketTimeoutException t) {
                        timeoutCount++;
                        Log.verb("Timeout on packet rcv " + timeoutCount);

                        if (timeoutCount > MAXSILENCE) {
                            Log.debug("Timeouts on packet rcv " + timeoutCount);
                            Log.debug("assuming consent revoked");
                            _rcv = null;
                        }
                    } 
                } catch (Exception x) {
                    if (_sock.isClosed()) {
                        _rcv = null;
                        Log.debug("Ice Socket closed quitting rcv loop");
                    }
                    Log.warn("Exception in ICE rcv loop");

                    if (Log.getLevel() >= Log.DEBUG) {
                        Log.warn("Exception in ICE rcv loop");
                        x.printStackTrace(System.out);
                    }
                }
            }
            Log.debug("quit ICE rcv loop");
            _send = null; // kill our partner
            _trans.getTransport().disconnected(selected);
        } catch (SocketException ex) {
            Log.error("Can't set timer in rcv loop");
        }
    }

    private void sendloop() {
        while (_send != null) {
            int snooze = Ta;
            try {
                long now = System.currentTimeMillis();
                List<StunPacket> tos = null;
                //synchronized (_trans) {
                tos = _trans.transact(now);
                //}
                for (StunPacket sp : tos) {
                    if (sp != null) {
                        byte o[] = sp.outboundBytes(miPass);
                        InetSocketAddress far = sp.getFar();
                        if (far != null) {
                            Log.verb(StunPacket.hexString(sp.getTid()) + " sending packet type " + sp.getClass().getSimpleName() + " length " + o.length + "  to " + far);
                            DatagramPacket out = new DatagramPacket(o, 0, o.length, far);
                            _sock.send(out);
                            // rate limit this here....
                            // Thread.sleep(Ta);
                        } else {
                            Log.verb("not sending packet to unresolved address");
                        }
                    }
                }
                synchronized (_trans) {
                    selected = _trans.findValidNominatedPair();
                    _trans.removeComplete();
                    long next = _trans.nextDue();
                    snooze = (int) (next - now);
                    if (snooze > Ta) {
                        _trans.wait(snooze);
                        snooze = 0;
                    }
                }
            } catch (Exception x) {
                Log.error("Exception in loop" + x.getMessage());
                //if (Log.getLevel() >= Log.DEBUG) {
                x.printStackTrace();
                //}
            }
            if (snooze > 0) {
                // ensure that even if an exception happens, we still sleep a bit.
                try {
                    Thread.sleep(snooze);
                } catch (Throwable z) {;
                }
            }
        }
        _rcv = null; // kill our partner
    }

    @Override
    public boolean isStarted() {
        return _started;
    }

    @Override
    public StunTransactionManager getTransactionManager() {
        return this._trans;
    }

    @Override
    public void sendTo(byte[] buf, int off, int len, InetSocketAddress dtlsTo) throws IOException {
        DatagramPacket p = new DatagramPacket(buf, off, len, dtlsTo);
        if (_sock == null) {
            throw new IOException("null socket for ICE");
        }
        _sock.send(p);
    }

    public void stop() {
        _send = null;
        _rcv = null;
    }

    public int getMTU() {
        return mtu;
    }
}
