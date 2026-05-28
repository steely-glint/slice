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
import com.ipseorama.slice.stun.StunAttribute;
import com.ipseorama.slice.stun.StunBindingRequest;
import com.ipseorama.slice.stun.StunBindingResponse;
import com.ipseorama.slice.stun.StunBindingTransaction;
import com.ipseorama.slice.stun.StunPacket;
import com.ipseorama.slice.stun.StunPacketException;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;

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
    private final int mtu = StunPacket.MTU;
    private final Map<String, String> miPass = new HashMap();
    long nextAvailableTime;
    private RTCIceCandidatePair selected;
    private Long selectedAt = null;
    static int sliceid = 0;
    private boolean doSped;
    private final ArrayList<Integer> spedAckList = new ArrayList();
    private final ArrayList<byte[]> outboundDTLS = new ArrayList();

    @Override
    public synchronized void start(Selector s, StunTransactionManager tm) {
        if (_started) {
            throw new java.lang.IllegalStateException("Can't start a Threaded Ice engine more than once.");
        }
        nextAvailableTime = System.currentTimeMillis();
        _selector = s;
        _transM = tm;
        RTCIceTransport transP = tm.getTransport();
        if (transP != null) {
            Log.verb("we have a transport to work with");
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
                        selectedAt = System.currentTimeMillis();
                        _transM.pruneExcept(selected, selectedAt + 250); // clear near future stuff too.
                        //removeUnselectedChannels(); // that's not really RFC 
                        Log.debug("SELECTED ->> " + selected);
                        //selected.pushDTLSStash();
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
        } else {
            throw new java.lang.IllegalStateException("Ice engine must have a transport");
        }

        if ((_selector == null) || (_transM == null)) {
            throw new java.lang.IllegalArgumentException("Need non-null selector and transaction manager to start");
        }

        Runnable ior = () -> {
            loop();
        };
        _rcv = new Thread(ior, "nio-ice-" + sliceid + ":");
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
                if ((selected == null) && (Log.getLevel() > Log.DEBUG)) {
                    Log.debug("-----> candidate Pair States <------");
                    _transM.listPairs();
                }
                rx(delay);
            }
            Log.debug("quit ICE rcv loop");
            RTCIceTransport t = _transM.getTransport();
            if (t != null) {
                t.disconnectedSelected();
            } else {
                Log.debug("no transport. Selected is: " + selected);
                if (Log.getLevel() >= Log.DEBUG) {
                    _transM.listPairs();
                }
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
        ((Buffer) recbuf).flip();
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
        RTCIceTransport transport = _transM.getTransport();
        if ((b < 2) && (b >= 0)) {
            StunPacket rp = StunPacket.mkStunPacket(rec, miPass, near, _transM);
            rp.setFar(far);
            rp.setChannel(dgc);
            Log.verb(StunPacket.hexString(rp.getTid()) + "got packet type " + rp.getClass().getSimpleName() + " from " + far);
            _transM.receivedPacket(rp, RTCIceProtocol.UDP, ipv);
            if (doSped) {
                inboundSped(rp, transport);
            }
        } else if ((19 < b) && (b < 64)) {
            if (selected == null) {
                RTCIceCandidatePair pair = transport.findCandiatePair(dgc, far);
                if (pair != null) {
                    transport.setInbound(pair);
                    Log.debug("didn't stash DTLS packet - used inbound pair " + pair);
                } else {
                    Log.warn("No matching pair found for dtls ?!?!");
                    Log.debug("DTLS came in on channel " + dgc.toString() + " far " + far);
                    _transM.getTransport().listPairs();
                }
            }
            transport.pushDTLS(rec);
            Log.debug("push inbound DTLS packet");
        } else if (b < 0) {
            if (selected != null) {
                DatagramPacket dgp = new DatagramPacket(new byte[0], 0);
                dgp.setSocketAddress(far);
                dgp.setData(rec);
                dgp.setLength(len);
                transport.pushRTP(dgp);
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
        if (this.selected == null) {
            Log.debug("delay is  =" + delay);
        }

        try {

            int keys = _selector.select(delay);
            Log.verb("Selection key count =" + keys);
            Set<SelectionKey> selectedKeys = _selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                while (key.isValid() && key.isReadable()) {
                    try {
                        if (!readPacket(key)) {
                            break;
                        }
                    } catch (StunPacketException mex) {
                        // non fatal exception
                        Log.debug("Weird Stun packet - ignoring it... " + mex.getClass().getCanonicalName() + " " + mex.getMessage());
                        Log.debug("packet was " + mex.getPacket());
                        Log.debug("packet contained " + mex.getPacket().listAttribs());
                    } catch (java.net.PortUnreachableException pox) {
                        if (selectedAt != null) {
                            if ((now - selectedAt) > StunBindingTransaction.MAXTIME) {
                                Log.info("ICMP long after selection - assuming channel has gone.");
                                cancelChannel(key, pox);
                                throw (pox);
                            }
                        } else {
                            Log.debug("Port unreachable exception on " + key.attachment() + " message " + pox.getMessage());
                        }
                    } catch (Exception x) {
                        // all other exceptions close the channel.
                        cancelChannel(key, x);
                        Log.warn("Exception " + x + " on packet recv. Not closing dgc ");
                        x.printStackTrace();
                        // to do - strictly we should tell the candidatePair that it has gone bad...
                        throw (x); // see if there are any channels left to listen on.
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
            boolean haveValid = false;
            if (_selector.isOpen()) {
                Optional<SelectionKey> valid = _selector.keys().stream().filter((SelectionKey key) -> {
                    Log.debug("channel attached to " + key.attachment());
                    return key.isValid() && (key.channel().isOpen());
                }).findAny();
                if (valid.isPresent()) {
                    Object att = valid.get().attachment();
                    Log.debug("Still have valid channel on " + att + " not giving up...");
                    haveValid = true;
                }
            }
            if (!haveValid) {
                _rcv = null;
                Log.debug("Ice Selector out of valid channels,  quitting rcv loop");
            }

            if (Log.getLevel() >= Log.DEBUG) {
                Log.warn("Exception in ICE rcv loop");
                x.printStackTrace(System.out);
            }
        }
    }

    private void cancelChannel(SelectionKey key, Exception x) {
        Log.debug("Cancelling " + key.attachment() + " because " + x.toString());
        if (key.isValid()) {
            key.cancel();
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
                        if (doSped) {
                            outboundSped(sp);
                        }
                        byte o[] = sp.outboundBytes(miPass);
                        DatagramChannel ch = sp.getChannel();
                        InetSocketAddress far = sp.getFar();
                        if (far != null) {
                            Log.verb(StunPacket.hexString(sp.getTid()) + " sending packet type " + sp.getClass().getSimpleName() + " length " + o.length + "  to " + far);
                            ByteBuffer src = ByteBuffer.wrap(o);
                            if (ch.isOpen()) {
                                try {
                                    if (ch.isConnected()) {
                                        ch.write(src);
                                    } else {
                                        ch.send(src, far);
                                    }
                                } catch (java.net.PortUnreachableException pux) {
                                    Log.warn("port unreachable for outbound -> close()" + sp.getFar());
                                    ch.close();
                                } catch (java.net.NoRouteToHostException pux) {
                                    Log.warn("no route to host for outbound -> close()" + sp.getFar());
                                    ch.close();
                                }
                            } else {
                                Log.debug("chanel is closed " + ch);
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
            for (StackTraceElement el : trace) {
                Log.warn("\n\t " + el.toString());
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

    public void removeUnselectedChannels() {
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
                Log.debug("keeping selected channel " + kc);
            }
        });
    }

    public void doSped(boolean b) {
        this.doSped = b;
    }

    private int crcOf(byte[] data) {
        byte b[] = new byte[4];
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }

    /*
    3.3.2.1. DTLS-IN-STUN-DATA

This attribute contains one DTLS handshake packet, or is empty to indicate SPED support when no DTLS packet is being embedded.
While SPED is active, this attribute MUST be present in every STUN Binding Request or Response sent by a SPED-capable agent.
The value portion of this attribute is variable length and consists of one DTLS handshake packet from a DTLS flight, as described in Section 5.1 of [RFC9147] or Section 4.2 of [RFC6347].
As noted, if the attribute length is not a multiple of 4, padding must be added.
If the value portion of this attribute is empty, it indicates SPED support and that no DTLS packet is being embedded in that STUN message. An empty value MUST NOT be injected into the DTLS layer.
If the value portion of this attribute is non-empty but the first byte is not DTLS, i.e. between 20 and 63 inclusive as described in Section 3 of [RFC9443], the attribute SHOULD be silently discarded.

    3.3.2.2. DTLS-IN-STUN-ACK

This attribute contains acknowledgements of received DTLS-IN-STUN-DATA packets in the order they were received.
The attribute can be present in either a STUN Binding Request or Response.
The attribute is variable length and contains a list of uint32 entries, where each entry is the computed CRC-32 of a received DTLS-IN-STUN-DATA attribute value, i.e. a DTLS handshake packet, ignoring padding.
Implementations SHOULD cap the number of uint32 entries included in this attribute. A cap of 4 entries is RECOMMENDED, which bounds the attribute size while still covering all known handshake cases.
The attribute can be empty, i.e. the length of the list of uint32 values can be 0.
     */
    private void inboundSped(StunPacket rp, RTCIceTransport transport) {
        if (rp.hasAttribute("DTLS-IN-STUN-DATA")) {
            StunAttribute dataA = rp.getAttributeByName("DTLS-IN-STUN-DATA");
            byte[] data = dataA.getBytes();
            if ((data.length > 0) && ((data[0] >= 20) && (data[0] <= 63))) {
                Integer acc = crcOf(data);
                boolean push = false;
                synchronized (spedAckList) {
                    if (!spedAckList.contains(acc)) {
                        spedAckList.add(acc);
                        push = true;
                    }
                }
                if (push) {
                    Log.debug("Pushing sped data to DTLS " + acc);
                    transport.pushDTLS(data);
                } else {
                    Log.debug("Not Pushing sped data to DTLS " + acc);
                }
            } else {
                Log.debug("not Pushing sped data to DTLS " + data.length);
            }
        }
        if (rp.hasAttribute("DTLS-IN-STUN-ACK")) {
            StunAttribute dataA = rp.getAttributeByName("DTLS-IN-STUN-ACK");
            byte[] data = dataA.getBytes();
            int len = data.length / 4;
            ByteBuffer crcs = ByteBuffer.wrap(data);
            for (int i = 0; i < len; i++) {
                long crc = crcs.getInt();
                Log.debug("far saw sped DTLS " + crc);
                synchronized (outboundDTLS) {
                    this.outboundDTLS.removeIf((bo) -> {
                        int lcrc = crcOf(bo);
                        boolean res = lcrc == crc;
                        Log.verb("sped DTLS checking ack against " + lcrc + " remove ->" + res);
                        return res;
                    });
                }
            }
        }
    }

    // how to do flights?
    
    private void outboundSped(StunPacket sp) {
        if ((sp instanceof StunBindingRequest) || (sp instanceof StunBindingResponse)) {
            byte[] pk;
            synchronized (outboundDTLS) {
                pk = outboundDTLS.isEmpty() ? new byte[0] : outboundDTLS.getFirst();
            }
            ByteBuffer acks;
            synchronized (spedAckList) {
                acks = ByteBuffer.allocate(spedAckList.size() * 4);
                spedAckList.forEach((l) -> acks.putInt((int) (long) l));
            }
            sp.addSpedAttribute("DTLS-IN-STUN-DATA", pk);
            sp.addSpedAttribute("DTLS-IN-STUN-ACK", acks.array());
            Log.debug("Added adding outbound DTLS sped to " + sp);

        } else {
            Log.debug("Not adding DTLS sped to " + sp);
        }
    }

    public void addOutBoundDTLS(byte[] buf, int off, int len) {
        byte[] b = new byte[len];
        System.arraycopy(buf, off, b, 0, len);
        Log.debug("sped DTLS enqueued " + crcOf(b));
        synchronized (outboundDTLS) {
            outboundDTLS.add(b);
        }
    }

}
