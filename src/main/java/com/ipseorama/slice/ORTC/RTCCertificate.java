/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import java.util.function.Consumer;

/**
 *
 * @author tim
 */
public interface RTCCertificate {

    long getExpires();
    RTCDtlsFingerprint getFingerprint();
    String getAlgorithm();
    void generateCertificate(String keygenAlgorithm, Consumer <RTCCertificate> cons);
    
}
