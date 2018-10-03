/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.phono.srtplight.Log;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 * @author tim
 */
public class RTCIceCandidate implements RTCIceGatherCandidate, RTCEventData {

    private String foundation;
    private Long priority;
    private String ip;
    private RTCIceProtocol protocol;
    private char port;
    private RTCIceCandidateType type;
    private RTCIceTcpCandidateType tcpType;
    private String relatedAddress;
    private char relatedPort;
    private int generation;
    private int ipVersion = 6;
    private int mtu;

    static RTCIceCandidate mkTempCandidate(InetSocketAddress isoc, RTCIceProtocol prot, int ipversion, long pri) {
        InetAddress home = isoc.getAddress();
        String found = RTCIceCandidate.calcFoundation(RTCIceCandidateType.PRFLX, home, null, prot);
        RTCIceCandidate cand = new RTCIceCandidate(found,
                pri,
                home.getHostAddress(),
                RTCIceProtocol.UDP,
                (char) isoc.getPort(),
                RTCIceCandidateType.HOST,
                null);
        cand.setIpVersion(ipversion);
        return cand;
    }

    /*String              relatedAddress;
             char         relatedPort;*/
    public RTCIceCandidate(String foundation,
            long priority,
            String ip,
            RTCIceProtocol protocol,
            char port,
            RTCIceCandidateType type,
            RTCIceTcpCandidateType tcpType) {
        this.foundation = foundation;
        this.priority = priority;
        this.ip = ip;
        this.protocol = protocol;
        this.port = port;
        this.type = type;
        this.tcpType = tcpType;
        this.generation = 0;
    }

    /**
     * @return the foundation
     */
    public String getFoundation() {
        return foundation;
    }

    /**
     * @param foundation the foundation to set
     */
    public void setFoundation(String foundation) {
        this.foundation = foundation;
    }

    /**
     * @return the priority
     */
    public long getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(long priority) {
        this.priority = priority;
    }

    /**
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * @return the protocol
     */
    public RTCIceProtocol getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(RTCIceProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the port
     */
    public char getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(char port) {
        this.port = port;
    }

    /**
     * @return the type
     */
    public RTCIceCandidateType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(RTCIceCandidateType type) {
        this.type = type;
    }

    /**
     * @return the tcpType
     */
    public RTCIceTcpCandidateType getTcpType() {
        return tcpType;
    }

    /**
     * @param tcpType the tcpType to set
     */
    public void setTcpType(RTCIceTcpCandidateType tcpType) {
        this.tcpType = tcpType;
    }

    /**
     * @return the relatedAddress
     */
    public String getRelatedAddress() {
        return relatedAddress;
    }

    /**
     * @param relatedAddress the relatedAddress to set
     */
    public void setRelatedAddress(String relatedAddress) {
        this.relatedAddress = relatedAddress;
    }

    /**
     * @return the relatedPort
     */
    public char getRelatedPort() {
        return relatedPort;
    }

    /**
     * @param relatedPort the relatedPort to set
     */
    public void setRelatedPort(char relatedPort) {
        this.relatedPort = relatedPort;
    }

    String toSDP(RTCIceComponent component) {
        int cno = (component == component.RTP) ? 1 : 2;
        StringBuffer ret = new StringBuffer("candidate:");
        ret.append(this.foundation).append(" ");
        ret.append(cno).append(" ");
        ret.append(protocol).append(" ");
        ret.append(priority).append(" ");
        ret.append(ip).append(" ");
        ret.append((int) port).append(" ");
        ret.append("typ ").append(type).append(" ");
        ret.append("generation ").append(generation).append(" ");
        if (this.relatedAddress != null) {
            ret.append("raddr ").append(relatedAddress).append(" ");
            ret.append("rport ").append("" + (int) relatedPort).append(" ");
            // raddr 192.67.4.33 rport 60003
        }
        // candidate:1365833797 1 udp 2113939711 2a01:348:339::57a:65d1:a0b4:a83 51309 typ host generation 0
        return ret.toString().trim();
    }

    public String toString() {
        return this.toSDP(RTCIceComponent.RTP);
    }

    boolean sameEnough(RTCIceCandidate cand) {
        boolean ret = this.getIp().equals(cand.getIp()) && this.getPort() == cand.getPort() && this.getProtocol().equals(cand.getProtocol());
        if (ret) {
            Log.verb("candidates are similar " + this + " vs " + cand);
        }
        return ret;
    }

    /*
    If these concerns are important, the
   type preference for relayed candidates SHOULD be lower than host
   candidates.  The RECOMMENDED values are 126 for host candidates, 100
   for server reflexive candidates, 110 for peer reflexive candidates,
   and 0 for relayed candidates.  Furthermore, if an agent is multihomed
   and has multiple IP addresses, the local preference for host
   candidates from a VPN interface SHOULD have a priority of 0.

   Another criterion for selection of preferences is IP address family.
   ICE works with both IPv4 and IPv6.  It therefore provides a
   transition mechanism that allows dual-stack hosts to prefer
   connectivity over IPv6, but to fall back to IPv4 in case the v6
   networks are disconnected (due, for example, to a failure in a 6to4
   relay) [RFC3056].  It can also help with hosts that have both a
   native IPv6 address and a 6to4 address.  In such a case, higher local
   preferences could be assigned to the v6 addresses, followed by the
   6to4 addresses, followed by the v4 addresses.  This allows a site to
   obtain and begin using native v6 addresses immediately, yet still
   fall back to 6to4 addresses when communicating with agents in other
   sites that do not yet have native v6 connectivity.
     */
    static int calcPriority(RTCIceCandidateType ctype, char localpref, RTCIceComponent comp) {
        int ctv = 0;
        switch (ctype) {
            case HOST:
                ctv = 126;
                break;
            case SRFLX:
                ctv = 100;
                break;
            case PRFLX:
                ctv = 110;
                break;
            case RELAY:
                ctv = 0;
                break;
        }
        int priority = (2 ^ 24) * (ctv)
                + (2 ^ 8) * (localpref)
                + (2 ^ 0) * (256 - comp.ordinal());
        return priority;

    }

    /*
    4.1.1.3.  Computing Foundations

   Finally, the agent assigns each candidate a foundation.  The
   foundation is an identifier, scoped within a session.  Two candidates
   MUST have the same foundation ID when all of the following are true:

   o  they are of the same type (host, relayed, server reflexive, or
      peer reflexive).

   o  their bases have the same IP address (the ports can be different).

   o  for reflexive and relayed candidates, the STUN or TURN servers
      used to obtain them have the same IP address.

   o  they were obtained using the same transport protocol (TCP, UDP,
      etc.).

   Similarly, two candidates MUST have different foundations if their
   types are different, their bases have different IP addresses, the
   STUN or TURN servers used to obtain them have different IP addresses,
   or their transport protocols are different.

     */
    static String calcFoundation(RTCIceCandidateType ctype, InetAddress base, InetAddress svr, RTCIceProtocol protocol) {
        String thing = ctype.name() + base.getHostAddress() + (svr == null ? "0.0.0.0" : svr.getHostAddress()) + protocol.name();
        String ret = Integer.toUnsignedString(thing.hashCode());
        return ret;
    }

    int getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(int v) {
        ipVersion = v;
    }

    boolean sameSocketAddress(InetSocketAddress sa) {
        boolean ret = this.getIp().equals(sa.getAddress()) && this.getPort() == sa.getPort();
        return ret;
    }

    void setMTU(int m) {
        mtu = m;
    }
}
