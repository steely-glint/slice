/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCredentialType;
import java.net.URI;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCIceServer {

    public final List<URI> urls;
    public final String username;
    public final String credential;
    public final RTCIceCredentialType credentialType;

    public RTCIceServer(List<URI> u, String uname, String cred, RTCIceCredentialType credType) {
        urls = u;
        username = uname;
        credential = cred;
        credentialType = credType == null ? RTCIceCredentialType.PASSWORD : credType;
    }
}
