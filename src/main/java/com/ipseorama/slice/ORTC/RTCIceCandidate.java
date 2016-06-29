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

/**
 *
 * @author tim
 */
public class RTCIceCandidate implements RTCIceGatherCandidate,RTCEventData{

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


    /*String              relatedAddress;
             char         relatedPort;*/
    RTCIceCandidate(String foundation,
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
        int cno = (component == component.RTP )? 1:2;
        StringBuffer ret = new StringBuffer("candidate:");
        ret.append(this.foundation).append(" ");
        ret.append(cno).append(" ");
        ret.append(protocol).append(" ");
        ret.append(priority).append(" ");
        ret.append(ip).append(" ");
        ret.append((int)port).append(" ");
        ret.append("typ ").append(type).append(" ");
        ret.append("generation ").append(generation).append(" ");
        if (this.relatedAddress != null){
            ret.append("raddr ").append(relatedAddress).append(" ");
            ret.append("rport ").append(relatedPort).append(" ");
            // raddr 192.67.4.33 rport 60003
        }
        // candidate:1365833797 1 udp 2113939711 2a01:348:339::57a:65d1:a0b4:a83 51309 typ host generation 0
        return ret.toString().trim();
    }
    public String toString(){
        return this.toSDP(RTCIceComponent.RTP);
    }
}
