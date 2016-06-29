/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import com.phono.srtplight.Log;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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

    public RTCIceGatherer() {
        _localCandidates = new ArrayList();
        _component = RTCIceComponent.RTP;
        _state = RTCIceGathererState.NEW;
    }

    RTCIceComponent getIceComponent() {
        return getComponent();
    }
    private RTCIceGathererState _state;

    RTCIceGathererState getState() {
        return _state;
    }

    public void close() {
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
            while (nifs.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nifs.nextElement();
                byte[] hw = ni.getHardwareAddress();
                Log.debug("Adding interface: " + ni.getDisplayName());
                if (ni.isUp()) {
                    Enumeration ipads = ni.getInetAddresses();
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
                    if (home != null) {
                        localAdd.append(home.getHostAddress());
                        String foundation = "1";
                        long priority = 1; // to do
                        RTCIceCandidate cand4 = new RTCIceCandidate(foundation,
                                priority,
                                home.getHostAddress(),
                                RTCIceProtocol.UDP,
                                (char) _sock.getLocalPort(),
                                RTCIceCandidateType.HOST,
                                null);
                        addLocalCandidate(cand4);
                    }
                    if (home6 != null) {
                        localAdd.append(" [" + home6.getHostAddress() + "]");
                        String foundation = "1";
                        long priority = 1; // to do
                        RTCIceCandidate cand6 = new RTCIceCandidate(foundation,
                                priority,
                                home6.getHostAddress(),
                                RTCIceProtocol.UDP,
                                (char) _sock.getLocalPort(),
                                RTCIceCandidateType.HOST,
                                null);
                        addLocalCandidate(cand6);
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
        _localCandidates.add(cand);
        if (this.onlocalcandidate != null) {
            onlocalcandidate.onEvent(cand);
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
        gatherLocals();

    }

    public RTCIceParameters getLocalParameters() {
        return null;
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
    private void setState(RTCIceGathererState _state) {
        this._state = _state;
        if (this.onstatechange != null){
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
                break;
            } catch (SocketException ex) {
                Log.debug("retry with new port no " + pno + " is in use");
            }
        }
        return ret;
    }
}
