/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import static com.ipseorama.slice.stun.StunPacket.hasAttribute;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class StunIndication extends StunPacket {

    public StunIndication(short mtype, Integer fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity, InetSocketAddress near) {
        super(mtype, fingerprint, attributes, messageIntegrity, near);
    }

    public StunIndication() {
        super((short) 0x0010);
    }

    boolean hasRequiredAttributes() {
        String reqAttrs[] = {"FINGERPRINT"};
        for (String ra : reqAttrs) {
            if (!hasAttribute(this._attributes, ra)) {
                Log.debug("missing Attribute " + ra);
                return false;
            }
        }
        return true;
    }

    void setRequiredAttributes(int code, String reason) {

        ArrayList<StunAttribute> attrs = new ArrayList();

        StunAttribute a = new StunAttribute("FINGERPRINT");
        byte[] mi = new byte[20];
        a.setBytes(mi);
        attrs.add(a);


        this.setAttributes(attrs);
    }

}
