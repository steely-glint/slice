/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import java.nio.channels.DatagramChannel;

/**
 *
 * @author tim
 */
class RTCLocalIceReflexCandidate extends RTCLocalIceCandidate {

    public RTCLocalIceReflexCandidate(String foundation,
            long priority,
            String ip,
            RTCIceProtocol protocol,
            char port,
            RTCIceCandidateType type,
            RTCIceTcpCandidateType tcpType,
            DatagramChannel chan
    ) {
        super(foundation,
                priority,
                ip,
                protocol,
                port,
                type,
                tcpType,
                chan);
    }
    
}
