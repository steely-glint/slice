/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.phono.srtplight.Log;
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
        if (role.equals(RTCIceRole.CONTROLLING)){
            StunAttribute.addUseCandidate(attrs);
        }
        StunAttribute.addMessageIntegrity(attrs);
        StunAttribute.addFingerprint(attrs);

        bind.setAttributes(attrs);
    }

    String getUserName() {
        return outboundUser;
    }

    public boolean sentUseCandidate(){
        return role.equals(RTCIceRole.CONTROLLING);
    }
    @Override
    public void received(StunPacket r) {
        if (r instanceof StunBindingResponse) {
            response = (StunBindingResponse) r;
            if (response.hasRequiredAttributes()) {
                complete = true;
                if (oncomplete != null) {
                    oncomplete.onEvent(this);
                }
            } else {
                Log.debug("Ignored incomplete response");
            }
        } else {
            Log.warn("unexpected packet type into StunBinding transaction " + r.getClass().getSimpleName());
        }
    }
}
