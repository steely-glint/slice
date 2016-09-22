/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCDtlsRole;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCDtlsParameters {

    private RTCDtlsRole role;// = "auto";
    private List<RTCDtlsFingerprint> fingerprints;

    public RTCDtlsParameters(RTCDtlsRole r, RTCDtlsFingerprint f) {
        fingerprints = new ArrayList();
        fingerprints.add(f);
        role = r;
    }

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

}
