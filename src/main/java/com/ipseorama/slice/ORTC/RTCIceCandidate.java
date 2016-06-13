/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;

/**
 *
 * @author tim
 */
class RTCIceCandidate {

    private String foundation;
    private Long priority;
    private String ip;
    private RTCIceProtocol protocol;
    private char port;
    private RTCIceCandidateType type;
    private RTCIceTcpCandidateType tcpType;
    private String relatedAddress;
    private char relatedPort;

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
    public Long getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(Long priority) {
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
}
