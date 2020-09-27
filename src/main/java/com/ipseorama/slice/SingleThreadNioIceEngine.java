/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

import com.ipseorama.slice.ORTC.EventHandler;
import com.ipseorama.slice.ORTC.RTCIceCandidatePair;
import com.ipseorama.slice.ORTC.RTCIceTransport;
import com.ipseorama.slice.ORTC.RTCLocalIceCandidate;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceTransportState;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunPacketException;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author thp
 *
 * Simple experimental implementation of Ice engine
 *
 */
public class SingleThreadNioIceEngine implements IceEngine {

    StunTransactionManager _transM;
    private Selector _selector;
    private Thread _rcv;
    private boolean _started = false;
    static int POLL = 1000;
    static int MAXSILENCE = 40;
    static int Ta = 5;
    private int mtu = StunPacket.MTU;
    private Map<String, String> miPass = new HashMap();
    long nextAvailableTime;
    private RTCIceCandidatePair selected;
    static int sliceid=0;

    public synchronized void start(Selector s, StunTransactionManager tm) {
        if (_started) {
            throw new java.lang.IllegalStateException("Can't start a Threaded Ice engine more than once.");
        }
        nextAvailableTime = System.currentTimeMillis();
        _selector = s;
        _transM = tm;
        RTCIceTransport transP = tm.getTransport();
        if (transP != null) {
            EventHandler chain = transP.onstatechange;
            transP.onstatechange = (data) -> {
                if (chain != null) {
                    chain.onEvent(data);
                }
                RTCIceTransportState tstate = transP.getState();
                Log.debug("Transport state is now " + tstate.toString().toUpperCase());
                switch (tstate) {
                    case COMPLETED:
                        selected = transP.getSelectedCandidatePair();
                        _transM.pruneExcept(selected, System.currentTimeMillis());
                        removeUnselectedChannels(); // that's not really RFC 
                        Log.debug("SELECTED ->> " + selected);
                        selected.pushDTLSStash();
                        break;
                    case CONNECTED:
                        break;
                    case DISCONNECTED:
                    case FAILED:
                        // strictly these aren't synonyms. 
                        Log.debug("Failed ->> " + selected);
                        selected = null;
                        break;
                }
            };
        }

        if ((_selector == null) || (_transM == null)) {
            throw new java.lang.IllegalArgumentException("Need non-null selector and transaction manager to start");
        }

        Runnable ior = () -> {
            loop();
        };
        _rcv = new Thread(ior, "nio-ice-"+sliceid+":");
        sliceid++;
        _rcv.setPriority(Thread.MAX_PRIORITY);
        _rcv.start();
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

    long packetRxTime = 0;

    private void loop() {
        try {
            packetRxTime = System.currentTimeMillis();
            while (_rcv != null) {
                Long delay = tx();
                if (Log.getLevel() > Log.DEBUG) {
                    Log.debug("-----> candidate Pair States <------");
                    _transM.listPairs();
                }
                rx(delay);
            }
            Log.debug("quit ICE rcv loop");
            RTCIceTransport t = _transM.getTransport();
            if (t != null) {
                t.disconnectedSelected();
            }
        } catch (SocketException ex) {
            Log.error("Can't set timer in rcv loop");
        }
    }

    private boolean readPacket(SelectionKey key) throws Exception {
        ByteBuffer recbuf = ByteBuffer.allocate(mtu);
        DatagramChannel dgc = (DatagramChannel) key.channel();
        InetSocketAddress far = (InetSocketAddress) dgc.receive(recbuf);
        if (far == null) {
            // early return ----> 
            Log.verb("Empty read far address");
            return false;
        }
        InetSocketAddress near = (InetSocketAddress) dgc.getLocalAddress();
        int ipv = far.getAddress() instanceof java.net.Inet4Address ? 4 : 6;
        recbuf.flip();
        int len = recbuf.remaining();
        byte rec[] = new byte[len];
        recbuf.get(rec);
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
            StunPacket rp = StunPacket.mkStunPacket(rec, miPass, near, _transM);
            rp.setFar(far);
            rp.setChannel(dgc);
            Log.verb(StunPacket.hexString(rp.getTid()) + "got packet type " + rp.getClass().getSimpleName() + " from " + far);
            _transM.receivedPacket(rp, RTCIceProtocol.UDP, ipv);
        } else if ((19 < b) && (b < 64)) {
            Log.debug("push inbound DTLS packet");
            if (selected != null) {
                selected.pushDTLS(rec);
            } else {
                RTCIceCandidatePair pair = _transM.getTransport().findCandiatePair(dgc, far);
                if (pair != null) {
                    pair.stashPacket(rec);
                    Log.debug("stashed DTLS packet - no selected pair - yet...");
                } else {
                    Log.debug("No matching pair found ?!?!");
                    Log.debug("DTLS came in on channel "+dgc.toString()+" far "+far);
                    _transM.getTransport().listPairs();
                }
            }
        } else if (b < 0) {
            if (selected != null) {
                DatagramPacket dgp = new DatagramPacket(new byte[0], 0);

                dgp.setSocketAddress(far);
                dgp.setData(rec);
                dgp.setLength(len);
                selected.pushRTP(dgp);
            } else {
                // strictly this is wrong - we should stack them...
                Log.debug("dumping RTP packet - no selected pair - yet...");
            }
        } else {
            Log.verb("packet first byte " + b);
        }
        packetRxTime = System.currentTimeMillis();
        return true;
    }

    private void rx(Long nextTime) throws SocketException {
        //InetSocketAddress near = (InetSocketAddress) _sock.getLocalSocketAddress();
        int delay = POLL;
        long now = System.currentTimeMillis();

        if (nextTime != null) {
            delay = (int) (nextTime - now);
            if (delay <= 0) {
                delay = Ta;
            }
            if (delay > POLL) {
                delay = POLL;
            }
        }
        if (this.selected == null){
            Log.debug("delay is  =" + delay);
        }

        try {

            int keys = _selector.select(delay);
            Log.verb("Selection key count =" + keys);
            Set<SelectionKey> selectedKeys = _selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {

                SelectionKey key = iter.next();
                while (key.isReadable() && key.isValid()) {
                    try {
                        if (!readPacket(key)) {
                            break;
                        }
                    } catch (StunPacketException mex){
                        // non fatal exception
                        Log.warn("Weird Stun packet - ignoring it... "+mex.getClass().getCanonicalName()+" "+mex.getMessage());
                        Log.debug("packet was "+mex.getPacket());
                    } catch (java.net.PortUnreachableException pox){
                        Log.warn("Port unreachable exception on "+key.attachment()+" message "+pox.getMessage());
                    }catch (Exception x) {
                        // all other exceptions close the channel.
                        //Log.debug("Cancelling "+key.attachment()+" because "+x.toString());
                        //if (key.isValid()){
                        //    key.cancel();
                        //}
                        Log.warn("Exception "+x+" on packet recv. Not closing dgc ");
                        // to do - strictly we should tell the candidatePair that it has gone bad...
                        throw(x); // see if there are any channels left to listen on.
                    }
                }
                iter.remove();
            }

            long interval = now - packetRxTime;
            Log.verb("Time since packet rcv " + interval);

            if (interval > MAXSILENCE * POLL) {
                Log.debug("Time since packet rcv " + interval);
                Log.debug("assuming consent revoked");
                _rcv = null;
            }

        } catch (Exception x) {
            Optional<SelectionKey> valid = _selector.keys().stream().filter((SelectionKey key) -> {
                Log.debug("channel attached to " + key.attachment());
                return key.isValid() && (key.channel().isOpen());
            }).findAny();
            if (valid.isPresent()) {
                Object att = valid.get().attachment();
                Log.debug("Still have valid channel on " + att + " not giving up...");
            } else {
                _rcv = null;
                Log.debug("Ice Selector out of valid channels,  quitting rcv loop");
            }

            if (Log.getLevel() >= Log.DEBUG) {
                Log.warn("Exception in ICE rcv loop");
                x.printStackTrace(System.out);
            }
        }
    }

    private Long tx() {
        try {
            long now = System.currentTimeMillis();
            List<StunPacket> tos = null;
            tos = _transM.transact(now);
            if (tos != null) {
                for (StunPacket sp : tos) {
                    if (sp != null) {
                        byte o[] = sp.outboundBytes(miPass);
                        DatagramChannel ch = sp.getChannel();
                        InetSocketAddress far = sp.getFar();
                        if (far != null) {
                            Log.verb(StunPacket.hexString(sp.getTid()) + " sending packet type " + sp.getClass().getSimpleName() + " length " + o.length + "  to " + far);
                            ByteBuffer src = ByteBuffer.wrap(o);
                            if (ch.isConnected()) {
                                ch.write(src);
                            } else {
                                ch.send(src, far);
                            }
                        } else {
                            Log.verb("not sending packet to unresolved address");
                        }
                    }
                }
            }
            if (((tos == null) || tos.isEmpty()) && (selected == null)) {
                _transM.makeWork();
            }
        } catch (Exception x) {
            Log.error("Exception in tx " + x);
            StackTraceElement[] trace = x.getStackTrace();
            for (StackTraceElement el:trace){
                Log.warn("\n\t "+el.toString());
            }
        }
        long nextTime = _transM.nextDue();
        Log.verb("not removing complete ICE stun transactions for now... ");
        return nextTime;
    }

    @Override
    public boolean isStarted() {
        return _started;
    }

    @Override
    public StunTransactionManager getTransactionManager() {
        return this._transM;
    }

    public void stop() {
        _rcv = null;
    }

    public int getMTU() {
        return mtu;
    }

    private void removeUnselectedChannels() {
        // this isn't correct webRTC behaviour - but really accepting data on a
        // unselected channels is a mistake.
        DatagramChannel sc = selected.getLocal().getChannel();
        Set<SelectionKey> keys = _selector.keys();
        keys.forEach((key) -> {
            SelectableChannel kc = key.channel();
            if (kc != sc) {
                Object cand = key.attachment();
                if ((cand != null) && (cand instanceof RTCLocalIceCandidate)) {
                    Log.debug("cancelling channel on " + cand.toString());
                } else {
                    Log.debug("cancelling " + kc.toString());
                }
                key.cancel();
            } else {
                Log.debug("keeping selected channel "+kc );
            }
        });
    }

}
