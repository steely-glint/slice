/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCDtlsTransportState;
import com.phono.srtplight.Log;
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

    public RTCDtlsTransport(RTCIceTransport transport, List<RTCCertificate> certificates){
        
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

    
    public void start(RTCDtlsParameters remoteParameters) {
        Log.debug("would start DTLS here... ");
    }

    
    void stop() {
    }
    
    EventHandler onstatechange;
    EventHandler onerror;

}
