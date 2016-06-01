/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import java.util.List;

/**
 *
 * @author tim
 */
class RTCDtlsParameters {
             private RTCDtlsRole                  role;// = "auto";
             private List<RTCDtlsFingerprint> fingerprints;

    /**
     * @return the role
     */
    public RTCDtlsRole getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(RTCDtlsRole role) {
        this.role = role;
    }

    /**
     * @return the fingerprints
     */
    public List<RTCDtlsFingerprint> getFingerprints() {
        return fingerprints;
    }

    /**
     * @param fingerprints the fingerprints to set
     */
    public void setFingerprints(List<RTCDtlsFingerprint> fingerprints) {
        this.fingerprints = fingerprints;
    }
}
