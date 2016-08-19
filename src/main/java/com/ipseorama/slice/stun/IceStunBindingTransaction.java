/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import java.util.ArrayList;

/**
 *
 * @author tim
 */
public class IceStunBindingTransaction extends StunBindingTransaction {

    private final int reflexPri;
    private final RTCIceRole role;
    private final long tiebreaker;
    private final String outboundUser;

    public IceStunBindingTransaction(String host, int port,
            int reflexPri,
            RTCIceRole role,
            long tiebreaker,
            String outboundUser) {
        super(host, port);
        this.reflexPri = reflexPri;
        this.role = role;
        this.tiebreaker = tiebreaker;
        this.outboundUser = outboundUser;
    }

    @Override
    public StunPacket buildOutboundPacket() {
        StunPacket bind = super.buildOutboundPacket();
        if (bind != null) {
            populateAttributes(bind);
        }
        return bind;
    }

    private void populateAttributes(StunPacket bind) {

        ArrayList<StunAttribute> attrs = new ArrayList();
        StunAttribute.addPriority(attrs, reflexPri);
        StunAttribute.addUsername(attrs, outboundUser);
        StunAttribute.addSoftware(attrs);
        StunAttribute.addIceCon(attrs, role, tiebreaker);
        StunAttribute.addMessageIntegrity(attrs);
        StunAttribute.addFingerprint(attrs);

        bind.setAttributes(attrs);
    }

    String getUserName() {
        return outboundUser;
    }

}
