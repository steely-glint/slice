/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCDtlsTransportState;
import java.beans.EventHandler;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCDtlsTransport extends RTCStatsProvider {

    List<RTCCertificate> certificates;
    RTCIceTransport transport;
    RTCDtlsTransportState state;

    RTCDtlsTransport(RTCIceTransport transport, List<RTCCertificate> certificates){
        
    }
    
    RTCDtlsParameters getLocalParameters() {
        return null;
    }

    
    RTCDtlsParameters getRemoteParameters() {
        return null;
    }

    
    List<byte[]> getRemoteCertificates() {
        return null;
    }

    
    void start(RTCDtlsParameters remoteParameters) {
    }

    
    void stop() {
    }
    
    EventHandler onstatechange;
    EventHandler onerror;
}
