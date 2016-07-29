/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

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

    public synchronized void start(DatagramSocket ds, StunTransactionManager tm) {
        if (_started) {
            throw new java.lang.IllegalStateException("Can't start a Threaded Ice engine more than once.");
        }
        _sock = ds;
        _trans = tm;
        if ((_sock == null) || (_trans == null)) {
            throw new java.lang.IllegalArgumentException("Need non-null socket and transaction manager to start");
        }

        Runnable ior = () -> {

            rcvloop();
        };
        _rcv = new Thread(ior, "ice-rcv" + _sock.toString());
        _rcv.start();
        Runnable ios = () -> {
            sendloop();
        };
        _send = new Thread(ios, "ice-send" + _sock.toString());
        _send.start();
        _started = true;

    }

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

    private void rcvloop() {
        try {
            Map<String, String> miPass = null;
            byte[] recbuf = new byte[StunPacket.MTU];
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
                        StunPacket rp = StunPacket.mkStunPacket(rec, miPass, near);
                        rp.setFar(far);
                        _trans.receivedPacket(rp, RTCIceProtocol.UDP, ipv);
                        synchronized (_sock) {
                            // tell our friend that we may have some work for them.
                            _sock.notifyAll();
                        }
                    } catch (SocketTimeoutException t) {
                        ;// don't care
                    }
                } catch (Exception x) {
                    Log.error("Exception in rcv loop");
                    x.printStackTrace();
                }
            }
            _send = null; // kill our partner
        } catch (SocketException ex) {
            Log.error("Can't set timer in rcv loop");
        }
    }

    private void sendloop() {
        while (_rcv != null) {
            try {
                long now = System.currentTimeMillis();
                List<StunPacket> tos = _trans.transact(now);
                for (StunPacket sp : tos) {
                    byte o[] = sp.outboundBytes();
                    InetSocketAddress far = sp.getFar();
                    if (far != null) {
                        Log.verb("sending packet length " + o.length + "  to " + far);
                        DatagramPacket out = new DatagramPacket(o, 0, o.length, far);
                        _sock.send(out);
                    } else {
                        Log.verb("not sending packet to unresolved address");
                    }
                }
                long next = _trans.nextDue();
                int snooze = (int) (next - now);
                synchronized (_sock) {
                    _sock.wait(snooze);
                }
            } catch (Exception x) {
                Log.error("Exception in loop" + x.getMessage());
                if (Log.getLevel() >= Log.DEBUG) {
                    x.printStackTrace();
                }
            }
        }
        _rcv = null; // kill our partner
    }

    @Override
    public boolean isStarted() {
        return _started;
    }

}
