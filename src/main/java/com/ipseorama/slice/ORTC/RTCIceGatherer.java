/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.stun.StunBindingTransaction;
import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceGatherPolicy;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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
    DatagramSocket _sock; // should this be a property of the component ?
    ArrayList<RTCIceCandidate> _localCandidates;
    IceEngine _ice;
    private final StunTransactionManager _stm;
    private RTCIceParameters _localParams;

    public RTCIceGatherer() {
        _localCandidates = new ArrayList();
        _component = RTCIceComponent.RTP;
        _state = RTCIceGathererState.NEW;
        _stm = new StunTransactionManager();
    }

    RTCIceComponent getIceComponent() {
        return getComponent();
    }
    private RTCIceGathererState _state;

    public RTCIceGathererState getState() {
        return _state;
    }

    public void close() {
        if ((_sock != null) && (!_sock.isClosed())) {
            _sock.close();
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
                NetworkInterface ni = (NetworkInterface) nifs.nextElement();
                byte[] hw = ni.getHardwareAddress();
                Log.debug("Adding interface: " + ni.getDisplayName());
                if (ni.isUp()) {
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
                    localAdd.append(" " + ni.getDisplayName() + " ");
                    if (home6 != null) {
                        localAdd.append(" [" + home6.getHostAddress() + "]");
                        String foundation = RTCIceCandidate.calcFoundation(RTCIceCandidateType.HOST, home, null, RTCIceProtocol.UDP);
                        long priority = RTCIceCandidate.calcPriority(RTCIceCandidateType.HOST, (char) lpref, RTCIceComponent.RTP); // to do
                        lpref -= 6;
                        RTCIceCandidate cand6 = new RTCIceCandidate(foundation,
                                priority,
                                home6.getHostAddress(),
                                RTCIceProtocol.UDP,
                                (char) _sock.getLocalPort(),
                                RTCIceCandidateType.HOST,
                                null);
                        cand6.setMTU(mtu);
                        addLocalCandidate(cand6);
                    }
                    if (home != null) {
                        localAdd.append(home.getHostAddress());
                        String foundation = RTCIceCandidate.calcFoundation(RTCIceCandidateType.HOST, home, null, RTCIceProtocol.UDP);
                        long priority = RTCIceCandidate.calcPriority(RTCIceCandidateType.HOST, (char) lpref, RTCIceComponent.RTP); // to do
                        lpref -= 4;
                        RTCIceCandidate cand4 = new RTCIceCandidate(foundation,
                                priority,
                                home.getHostAddress(),
                                RTCIceProtocol.UDP,
                                (char) _sock.getLocalPort(),
                                RTCIceCandidateType.HOST,
                                null);
                        cand4.setIpVersion(4);
                        cand4.setMTU(mtu);
                        addLocalCandidate(cand4);
                    }

                } else {
                    Log.debug("Ignoring interface: " + ni.getDisplayName());
                }
            }
        } catch (SocketException x) {
        }

        for (RTCIceCandidate c : _localCandidates) {
            Log.debug(c.toSDP(_component));
        }

    }

    void addLocalCandidate(RTCIceCandidate cand) {
        if (!_localCandidates.stream().anyMatch((RTCIceCandidate ec) -> {
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
        _sock = allocateUdpSocket(options.getPortMin(), options.getPortMax());
        RTCIceGatherPolicy policy = options.getGatherPolicy();
        List<RTCIceServer> servers = options.getIceServers();
        if ((_ice != null) && (!_ice.isStarted())) {
            _ice.start(_sock, _stm);
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
        }
    }

    public RTCIceParameters getLocalParameters() {
        return _localParams;
    }

    public void setLocalParameters(RTCIceParameters local) {
        _localParams = local;
    }

    List<RTCIceCandidate> getLocalCandidates() {
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

    private DatagramSocket allocateUdpSocket(int portMin, int portMax) {
        DatagramSocket ret = null;
        SecureRandom rand = new SecureRandom();
        int rangeSz = portMax - portMin;
        for (int tries = 0; tries < rangeSz * 2; tries++) {
            int pno = portMin + rand.nextInt(rangeSz);
            try {
                ret = new DatagramSocket(pno);
                ret.setTrafficClass(0x10);
                break;
            } catch (SocketException ex) {
                Log.debug("retry with new port no " + pno + " is in use");
            }
        }
        return ret;
    }

    private void gatherReflex(List<RTCIceServer> servers) {

        servers.stream().forEach(
                (RTCIceServer s) -> {
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

                            sbt.oncomplete = (RTCEventData e) -> {
                                Log.debug("got binding reply - or timeout");
                                if (e instanceof RTCTimeoutEvent) {
                                    Log.debug("got binding timeout");
                                }
                                if (e instanceof StunBindingTransaction) {

                                    StunBindingTransaction st = (StunBindingTransaction) e;
                                    InetSocketAddress ref = st.getReflex();

                                    if (ref != null) {
                                        RTCIceCandidateType type = RTCIceCandidateType.SRFLX;
                                        RTCIceProtocol prot = RTCIceProtocol.UDP;
                                        InetAddress raddr = _sock.getLocalAddress();
                                        char rport = (char) _sock.getLocalPort();

                                        String foundation = RTCIceCandidate.calcFoundation(type, raddr, st.getFar().getAddress(), prot);
                                        long priority = RTCIceCandidate.calcPriority(RTCIceCandidateType.HOST, (char) (ref.getPort() / 2), RTCIceComponent.RTP);
                                        RTCIceCandidate cand4 = new RTCIceCandidate(foundation,
                                                priority,
                                                ref.getAddress().getHostAddress(),
                                                prot,
                                                (char) ref.getPort(),
                                                type,
                                                null);
                                        cand4.setRelatedAddress(raddr.getHostAddress());
                                        cand4.setRelatedPort(rport);
                                        if (ref.getAddress() instanceof java.net.Inet4Address) {
                                            cand4.setIpVersion(4);
                                        }
                                        Optional<RTCIceCandidate> lad = _localCandidates.stream().filter((RTCIceCandidate l) -> {
                                            return l.getIpVersion() == cand4.getIpVersion();
                                        }).findFirst();
                                        lad.ifPresent((RTCIceCandidate l) -> {
                                            cand4.setRelatedAddress(l.getIp());
                                            cand4.setRelatedPort(l.getPort());
                                        });
                                        addLocalCandidate(cand4);
                                    }
                                }
                                _stm.removeComplete();
                                if (_stm.size() == 0) {
                                    setState(RTCIceGathererState.COMPLETE);
                                }
                            };
                            _stm.addTransaction(sbt);
                        }
                    });
                });
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
}
