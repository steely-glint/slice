/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCDtlsRole;
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
    private RTCDtlsParameters remoteParameters;
    private RTCDtlsParameters localParameters;

    public RTCDtlsTransport(RTCIceTransport transport, List<RTCCertificate> certificates, RTCDtlsParameters localParameters) {
        this.localParameters = localParameters;
        this.transport = transport;
        this.certificates = certificates;
    }

    public RTCDtlsParameters getLocalParameters() {
        return localParameters;
    }

    public RTCDtlsParameters getRemoteParameters() {
        return this.remoteParameters;
    }

    public List<byte[]> getRemoteCertificates() {
        return null;
    }

    public void start(RTCDtlsParameters remoteParameters) {
        this.remoteParameters = remoteParameters;
        Log.debug("would start DTLS here... ");
    }

    public void stop() {
    }

    public EventHandler onstatechange;
    public EventHandler onerror;

    
    
}
