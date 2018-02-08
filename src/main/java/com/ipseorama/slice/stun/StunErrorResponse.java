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

public class StunErrorResponse extends StunPacket {

    public StunErrorResponse(short mtype, Integer fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity, InetSocketAddress near) {
        super(mtype, fingerprint, attributes, messageIntegrity, near);
    }

    public StunErrorResponse() {
        super((short) 0x0110);
    }

    boolean hasRequiredAttributes() {
        String reqAttrs[] = {"FINGERPRINT", "MESSAGE-INTEGRITY", "ERROR-CODE"};
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

        StunAttribute a = new StunAttribute("ERROR-CODE");
        StunAttribute.ErrorAttribute ea = a.makeErrorAttribute(code, reason);
        a.setError(ea);
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

}
