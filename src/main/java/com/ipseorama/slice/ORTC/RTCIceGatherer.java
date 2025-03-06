package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.stun.StunBindingTransaction;
import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceGatherPolicy;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 *
 * @author tim
 * http://ortc.org/wp-content/uploads/2015/06/ortc.html#rtcicegatherer*
 */
public class RTCIceGatherer extends RTCStatsProvider {

    /*
    An RTCIceGatherer instance can be associated to multiple RTCIceTransport objects.
    The RTCIceGatherer does not prune local candidates until at least one RTCIceTransport object has become associated
    and all associated RTCIceTransport objects are in the "completed" or "failed" state.

As noted in [RFC5245] Section 7.1.2.2, an incoming connectivity check contains an ICE-CONTROLLING or ICE-CONTROLLED attribute,
    depending on the role of the ICE agent initiating the check. Since an RTCIceGatherer object does not have a role,
    it cannot determine whether to respond to an incoming connectivity check with a 487 (Role Conflict) error; however,
    it can validate that an incoming connectivity check utilizes the correct local username fragment and password,
    and if not, can respond with an 401 (Unauthorized) error, as described in [RFC5389] Section 10.1.2.

For incoming connectivity checks that pass validation,
    the RTCIceGatherer must buffer the incoming connectivity checks so as to be able to provide them
    to associated RTCIceTransport objects so that they can respond.
     */
    private RTCIceComponent _component;
    // DatagramSocket _sock; // should this be a property of the component ?
    Selector _selector;
    ArrayList<RTCLocalIceCandidate> _localCandidates;
    IceEngine _ice;
    private final StunTransactionManager _stm;
    private RTCIceParameters _localParams;
    private RTCIceGatherOptions _options;

    public RTCIceGatherer() {
        _localCandidates = new ArrayList();
        _component = RTCIceComponent.RTP;
        _state = RTCIceGathererState.NEW;
        _stm = new StunTransactionManager();
        try {
            _selector = Selector.open();
        } catch (IOException x) {
            Log.error("Can't make ice selector! " + x.getMessage());
        }
    }

    RTCIceComponent getIceComponent() {
        return getComponent();
    }
    private RTCIceGathererState _state;

    public RTCIceGathererState getState() {
        return _state;
    }

    public void close() {
        if ((_selector != null) && (_selector.isOpen())) {
            try {
                _selector.close();
            } catch (IOException x) {
                Log.error("Can't close ice selector! " + x.getMessage());
            }
        }
    }

    void gatherLocals() {

        StringBuffer localAdd = new StringBuffer("");

        // we need to be _lots_ smarter about v6 here 
        // we might chose to use the ipv6 address if it is routable
        // when the v4 address isn't
        // but for now we choose the last non-loopback v4 address
        // or if none the last v6 address.
        // which will at least work if we are in pure v6
        try {
            Enumeration nifs = NetworkInterface.getNetworkInterfaces();

            int i = 0;
            int mtu = 1400;
            int lpref = Character.MAX_VALUE;
            while (nifs.hasMoreElements()) {
                try {
                    NetworkInterface ni = (NetworkInterface) nifs.nextElement();
                    byte[] hw = ni.getHardwareAddress();
                    if (hw == null )  { hw = new byte[4];}
                    Log.debug("Adding interface: " + ni.getDisplayName());
                    if (ni.isLoopback()) {
                        Log.debug("Skipping loopback " + ni.getDisplayName());
                    } else if (ni.isUp()) {
                        mtu = ni.getMTU();
                        Enumeration ipads = ni.getInetAddresses();
                        List<InterfaceAddress> iads = ni.getInterfaceAddresses();
                        Inet4Address home = null;
                        Inet6Address home6 = null;
                        while (ipads.hasMoreElements()) {
                            InetAddress ipad = (InetAddress) ipads.nextElement();
                            if (!ipad.isLoopbackAddress()) {
                                if (ipad instanceof Inet4Address) {
                                    if (home == null) {
                                        home = (Inet4Address) ipad;
                                        Log.debug("Using address: " + ipad.getHostAddress());
                                    } else {
                                        Log.debug("Not Using address: " + ipad.getHostAddress());
                                    }
                                }
                                if (ipad instanceof Inet6Address) {
                                    if (ipad.isLinkLocalAddress()) {
                                        Log.debug("Not Using link local address: " + ipad.getHostAddress());
                                        continue;
                                    }
                                    if (home6 == null) {
                                        home6 = (Inet6Address) ipad;
                                        Log.debug("Using address (for now) : " + ipad.getHostAddress() + " " + howMacBased(ipad.getAddress(), hw));
                                    } else if (howMacBased(home6.getAddress(), hw) > howMacBased(ipad.getAddress(), hw)) {
                                        home6 = (Inet6Address) ipad;
                                        Log.debug("Prefer using address : " + ipad.getHostAddress() + " " + howMacBased(ipad.getAddress(), hw));
                                    } else {
                                        Log.debug("Don't prefer Using address: " + ipad.getHostAddress() + " " + howMacBased(ipad.getAddress(), hw));
                                    }

                                }
                            }
                        }
                        localAdd.append("\t\t" + ni.getDisplayName() + " ");
                        if (home6 != null) {

                            localAdd.append(" [" + home6.getHostAddress() + "]");
                            String foundation = RTCIceCandidate.calcFoundation(RTCIceCandidateType.HOST, home6, null, RTCIceProtocol.UDP);
                            long priority = RTCIceCandidate.calcPriority(RTCIceCandidateType.HOST, (char) lpref, RTCIceComponent.RTP); // to do
                            lpref -= 6;
                            String sixad = home6.getHostAddress();
                            Log.debug("os sixad is " + sixad);
                            if (sixad.contains("%")) {
                                String bits[] = sixad.split("%");
                                sixad = bits[0];
                                Log.debug("new sixad is " + sixad);
                            }
                            try {
                                DatagramChannel channel = createDatagramChannel(sixad);
                                int port = channel.socket().getLocalPort();
                                RTCLocalIceCandidate cand6 = new RTCLocalIceCandidate(foundation,
                                        priority,
                                        sixad,
                                        RTCIceProtocol.UDP,
                                        (char) port,
                                        RTCIceCandidateType.HOST,
                                        null, channel);
                                cand6.setMTU(mtu);

                                addLocalCandidate(cand6);

                                channel.register(_selector, SelectionKey.OP_READ, (Object) cand6);
                            } catch (IOException x) {
                                Log.error("Candidate creation failed for " + home6);
                            }

                        }
                        if (home != null) {
                            String fourad = home.getHostAddress();
                            try {
                                DatagramChannel channel = createDatagramChannel(fourad);
                                localAdd.append(fourad).append('\n');
                                String foundation = RTCIceCandidate.calcFoundation(RTCIceCandidateType.HOST, home, null, RTCIceProtocol.UDP);
                                long priority = RTCIceCandidate.calcPriority(RTCIceCandidateType.HOST, (char) lpref, RTCIceComponent.RTP); // to do
                                lpref -= 4;
                                int port = channel.socket().getLocalPort();

                                RTCLocalIceCandidate cand4 = new RTCLocalIceCandidate(foundation,
                                        priority,
                                        fourad,
                                        RTCIceProtocol.UDP,
                                        (char) port,
                                        RTCIceCandidateType.HOST,
                                        null,
                                        channel);
                                cand4.setIpVersion(4);
                                cand4.setMTU(mtu);

                                addLocalCandidate(cand4);
                                channel.register(_selector, SelectionKey.OP_READ, (Object) cand4);
                            } catch (IOException x) {
                                Log.error("Candidate creation failed for " + fourad);
                            }
                        }
                    } else {
                        Log.debug("Ignoring interface: " + ni.getDisplayName());
                    }
                } catch (Throwable x) {
                    Log.warn("interface problem  " + x);
                    if (Log.getLevel() > Log.DEBUG) {
                        x.printStackTrace();
                    }
                }
            }
        } catch (Exception x) {
            Log.warn("interface problem  " + x);
            if (Log.getLevel() > Log.DEBUG) {
                x.printStackTrace();
            }
        }

        Log.debug("Local addresses: " + localAdd);
        for (RTCIceCandidate c : _localCandidates) {
            Log.debug(c.toSDP(_component));
        }

    }

    void addLocalCandidate(RTCLocalIceCandidate cand) {
        if (!_localCandidates.stream().anyMatch((RTCLocalIceCandidate ec) -> {
            return ec.sameEnough(cand);
        })) {
            _localCandidates.add(cand);
            if (this.onlocalcandidate != null) {
                onlocalcandidate.onEvent(cand);
            }
        } else {
            Log.debug("Skipping candidate as similar candidate is already present " + cand);
        }
    }

    public void gather(RTCIceGatherOptions options) {
        /* we do 2 things here : 
        1) list the local interfaces and create candidates from them
        2) send a Stun probe to see what our reflexive address is and create a candidate from that.
        as candidates are generated, we fire events and add to the list.
         */
        setState(RTCIceGathererState.GATHERING);
        _options = options;
        //_sock = allocateUdpSocket(options.getPortMin(), options.getPortMax());
        RTCIceGatherPolicy policy = options.getGatherPolicy();
        List<RTCIceServer> servers = options.getIceServers();
        if ((_ice != null) && (!_ice.isStarted())) {
            _ice.start(_selector, _stm);
        }
        switch (policy) {
            case NOHOST:
                gatherReflex(servers);
                gatherRelay(servers);
                break;
            case ALL:
                gatherLocals();
                gatherReflex(servers);
                gatherRelay(servers);
                break;
            case RELAY:
                gatherRelay(servers);
                break;
            case LOCAL:
                gatherLocals();
                Log.debug("Locals only - nothing to wait for...");
                setState(RTCIceGathererState.COMPLETE);
                break;

        }
    }

    public RTCIceParameters getLocalParameters() {
        return _localParams;
    }

    public void setLocalParameters(RTCIceParameters local) {
        _localParams = local;
    }

    List<RTCLocalIceCandidate> getLocalCandidates() {
        return _localCandidates;
    }

    RTCIceGatherer createAssociatedGatherer() {
        return null;
    }
    public EventHandler onstatechange;
    public EventHandler onerror;
    public EventHandler onlocalcandidate;

    /**
     * @return the _component
     */
    public RTCIceComponent getComponent() {
        return _component;
    }

    /**
     * @param _component the _component to set
     */
    public void setComponent(RTCIceComponent _component) {
        this._component = _component;
    }

    /**
     * @param _state the _state to set
     */
    protected void setState(RTCIceGathererState _state) {
        this._state = _state;
        if (this.onstatechange != null) {
            onstatechange.onEvent(_state);
        }
    }

    private int howMacBased(byte[] address, byte[] hw) {
        // check the last 3 bytes of the address against the hardware address
        // if they match we 'prefer' not to use that since it tags us pretty badly

        int ret = 0;
        if (address.length >= hw.length) {
            int aoff = address.length - 3;
            int hoff = hw.length - 3;
            int n;
            for (n = 0; n < 3; n++) {
                if (hw[hoff + n] != address[aoff + n]) {
                    break;
                }
            }
            ret = n;
        }
        return ret;
    }
    // allocate an unused port from the acceptable range
/*
    private DatagramSocket allocateUdpSocket(int portMin, int portMax) {
        DatagramSocket ret = null;
        SecureRandom rand = new SecureRandom();
        int rangeSz = portMax - portMin;
        for (int tries = 0; tries < rangeSz * 2; tries++) {
            int pno = portMin + rand.nextInt(rangeSz);
            try {
                ret = new DatagramSocket(pno);
                ret.setTrafficClass(46);
                break;
            } catch (SocketException ex) {
                Log.debug("retry with new port no " + pno + " is in use");
            }
        }
        return ret;
    }
     */
    Stream<DatagramChannel> getChannelsForReflex() {
        Stream<DatagramChannel> nret = _localCandidates.stream().filter((RTCIceCandidate l) -> {
            return l.getIpVersion() == 4;
        }).map((RTCLocalIceCandidate las) -> {
            return las.getChannel();
        });
        return nret;
    }

    DatagramChannel getChannelForReflex() throws IOException {
        DatagramChannel ret = null;

        Optional<RTCLocalIceCandidate> lad = _localCandidates.stream().filter((RTCIceCandidate l) -> {
            return l.getIpVersion() == 4;
        }).findFirst();
        if (lad.isPresent()) {
            ret = lad.get().getChannel();
            Log.debug("reusing local candidate channel for STUN");
        }

        if (ret == null) {
            ret = createDatagramChannel("0.0.0.0");
            ret.register(_selector, SelectionKey.OP_READ);
            Log.debug("had to create new channel for STUN");
            //addButDontSendLocalCandidate(ret);
        }
        return ret;
    }

    private void gatherReflex(List<RTCIceServer> servers) {
        //DatagramChannel reflexC = getChannelForReflex();
        _localCandidates.stream().filter((RTCIceCandidate l) -> {
            Log.debug("local candidate is " + l);
            return l.getIpVersion() == 4;
        }).map((RTCLocalIceCandidate las) -> {
            Log.debug("local v4 candidate is " + las);
            return las.getChannel();
        }).forEach((DatagramChannel reflexC) -> {
            try {
                Log.debug("trying reflex channel " + reflexC.getLocalAddress().toString());
            } catch (Exception x) {
            };
            servers.stream().forEach((RTCIceServer s) -> {
                Log.debug("trying ICE server " + s.toString());
                Stream<String> stuns = s.urls.stream().map(
                        (URI u) -> {
                            String stun = "stun:";
                            String us = u.toASCIIString();
                            String hnp = null;
                            Log.verb("checking uri " + us);

                            if (us.toLowerCase().startsWith(stun)) {
                                hnp = us.substring(stun.length());
                                Log.verb("stun host " + hnp);
                            }
                            return hnp;
                        }
                ).filter((String host) -> {
                    Log.verb("stunhost is " + host);
                    return host != null;
                });
                stuns.forEach((String ss) -> {
                    Log.debug("trying stun server " + ss.toString());

                    String bits[] = ss.split(":");
                    String host = null;
                    int port = 3478;
                    if (bits.length == 2) {
                        host = bits[0];
                        port = Integer.parseInt(bits[1]);
                    }
                    if (bits.length == 1) {
                        host = bits[0];
                    }
                    if (host != null) {
                        StunBindingTransaction sbt = new StunBindingTransaction(_ice, host, port);
                        sbt.setCause("outbound gather");
                        sbt.setChannel(reflexC);

                        sbt.oncomplete = (RTCEventData e) -> {
                            Log.debug("got binding reply - or timeout");
                            if (e instanceof RTCTimeoutEvent) {
                                Log.debug("got binding timeout on " + sbt.toString());
                            }
                            if (e instanceof StunBindingTransaction) {

                                StunBindingTransaction st = (StunBindingTransaction) e;
                                InetSocketAddress ref = st.getReflex();

                                if (ref != null) {
                                    RTCIceCandidateType type = RTCIceCandidateType.SRFLX;
                                    RTCIceProtocol prot = RTCIceProtocol.UDP;
                                    InetAddress raddr = reflexC.socket().getLocalAddress();
                                    char rport = (char) reflexC.socket().getLocalPort();
                                    Log.debug("got stun reply on " + reflexC.socket() + " from " + st.getFar());
                                    String foundation = RTCIceCandidate.calcFoundation(type, raddr, st.getFar().getAddress(), prot);
                                    long priority = RTCIceCandidate.calcPriority(RTCIceCandidateType.HOST, (char) (ref.getPort() / 2), RTCIceComponent.RTP);
                                    RTCLocalIceCandidate rcand = new RTCLocalIceReflexCandidate(foundation,
                                            priority,
                                            ref.getAddress().getHostAddress(),
                                            prot,
                                            (char) ref.getPort(),
                                            type,
                                            null, reflexC);

                                    rcand.setRelatedAddress(raddr.getHostAddress());
                                    rcand.setRelatedPort(rport);
                                    if (ref.getAddress() instanceof java.net.Inet4Address) {
                                        rcand.setIpVersion(4);
                                    }
                                    try {
                                        InetSocketAddress l = (InetSocketAddress) reflexC.getLocalAddress();
                                        rcand.setRelatedAddress(l.getAddress().getHostAddress());
                                        rcand.setRelatedPort((char) l.getPort());
                                    } catch (IOException x) {
                                        Log.warn("reflex candidate without rhost/rport " + x.getMessage());
                                    }
                                    addLocalCandidate(rcand); // but don't pair it.
                                } else {
                                    Log.debug("already have ref ");
                                }
                            }
                            _stm.removeComplete();
                            if (_stm.size() == 0) {
                                setState(RTCIceGathererState.COMPLETE);
                            } else {
                                Log.debug(" still have " + _stm.size() + " transactions queued");
                            }
                        };
                        _stm.addTransaction(sbt);
                    }
                });
            });
        });
        if (_stm.size() == 0) {
            Log.debug("no stun stun transactions from our local addresses ");
            setState(RTCIceGathererState.COMPLETE);
        }
    }

    private void gatherRelay(List<RTCIceServer> servers) {
        // massive, massive to-do - Implement TURN UDP and TURN TLS
    }

    public void setIceEngine(IceEngine tie) {
        _ice = tie;
    }

    public IceEngine getIceEngine() {
        return _ice;
    }

    StunTransactionManager getStunTransactionManager() {
        return _stm;
    }

    private DatagramChannel createDatagramChannel(String home) throws IOException {
        DatagramChannel ret = DatagramChannel.open();

        int portMin = (_options != null) ? _options.getPortMin() : 9000;
        int portMax = (_options != null) ? _options.getPortMax() : 10000;
        SecureRandom rand = new SecureRandom();
        int rangeSz = portMax - portMin;
        for (int tries = 0; tries < rangeSz * 2; tries++) {
            int pno = portMin + rand.nextInt(rangeSz);
            try {
                InetSocketAddress local = new InetSocketAddress(home, pno);
                Log.debug("new local socket address " + local.toString());
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
}
