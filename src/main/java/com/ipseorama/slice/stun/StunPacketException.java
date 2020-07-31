/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

/**
 *
 * @author thp
 */
public class StunPacketException extends Exception {

    private StunPacket packet;

    static class FingerPrintException extends StunPacketException {

        public FingerPrintException() {
            super();
        }

        public FingerPrintException(String cause) {
            super(cause);
        }
    }

    static class MessageIntegrityException extends StunPacketException {

        public MessageIntegrityException() {
            super();
        }

        public MessageIntegrityException(String cause) {
            super(cause);
        }

        MessageIntegrityException(String no_pass_available, StunPacket ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public StunPacketException(String message) {
        this(message, null);
    }

    public StunPacketException(String message, StunPacket p) {
        super(message);
        packet = p;
    }

    public StunPacketException() {
        super();
    }

    public void setPacket(StunPacket s) {
        packet = s;
    }

    public StunPacket getPacket() {
        return packet;
    }
}
