/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

/**
 *
 * @author tim
 */
public class RTCIceParameters {

    final String usernameFragment;
    final String password;
    final boolean iceLite;

    public RTCIceParameters(String uF, String pass, boolean lite) {
        usernameFragment = uF;
        password = pass;
        iceLite = lite;
    }
}
