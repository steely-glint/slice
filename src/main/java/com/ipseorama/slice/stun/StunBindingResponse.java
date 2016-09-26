/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class StunBindingResponse extends StunPacket {

    public StunBindingResponse(short mtype, Integer fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity, InetSocketAddress near) {
        super(mtype, fingerprint, attributes, messageIntegrity, near);
    }

    public StunBindingResponse() {
        super((short) 0x0101);
    }

    InetSocketAddress getReflex() {
        InetSocketAddress i = null;
// prefer to use the xor address - but if it isn't there, use the plain one.
        try {
            Object[] iat = _attributes.stream().filter((StunAttribute a) -> {
                return ((a.getName() != null) && (a.getName().equals("XOR-MAPPED-ADDRESS")));
            }).toArray();
            if (iat.length < 1) {
                Log.warn("no xor-Address attributes in this BindingResponse");
                iat = _attributes.stream().filter((StunAttribute a) -> {
                    return ((a.getName() != null) && (a.getName().equals("MAPPED-ADDRESS")));
                }).toArray();
            }
            if (iat.length >= 1) {
                StunAttribute at = (StunAttribute) iat[0];
                Log.debug("Address attributes in this BindingResponse " + at.getName());
                i = (at.getName().equals("XOR-MAPPED-ADDRESS")) ? at.getXorIpAddress(_tid) : at.getIpAddress();
            } else {
                Log.warn("no Address attributes in this BindingResponse");
            }
        } catch (UnknownHostException ex) {
            Log.error("Problem getting socket address" + ex);
        }
        return i;
    }

    void setRequiredAttributes(InetSocketAddress far, String ufrags) {

        Log.verb("response username=" + ufrags);
        ArrayList<StunAttribute> attrs = new ArrayList();

        StunAttribute a = new StunAttribute("USERNAME");
        a.setString(ufrags);
        attrs.add(a);

        a = new StunAttribute("XOR-MAPPED-ADDRESS");
        a.setXorIpAddress(far);
        attrs.add(a);

        a = new StunAttribute("MESSAGE-INTEGRITY");
        byte[] mi = new byte[20];
        a.setBytes(mi);
        attrs.add(a);

        a = new StunAttribute("FINGERPRINT");
        a.setInt(0);
        attrs.add(a);

        this.setAttributes(attrs);
    }

    boolean hasRequiredAttributes() {
        String reqAttrs[] = {"FINGERPRINT", "MESSAGE-INTEGRITY", "XOR-MAPPED-ADDRESS"};
        for (String ra : reqAttrs) {
            if (!hasAttribute(this._attributes, ra)) {
                Log.debug("missing Attribute " + ra);
                return false;
            }
        }
        return true;
    }
}
