/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class StunErrorResponse extends StunPacket {

    public StunErrorResponse(short mtype, Integer fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity,InetSocketAddress near) {
        super(mtype, fingerprint, attributes, messageIntegrity,near);
    }

    public StunErrorResponse() {
        super((short) 0x0110);
    }
}
