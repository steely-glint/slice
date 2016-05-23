/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.CRC32;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author thp
 */
public class StunPacket {

    static int MILENGTH = 24;
    static int FPLEN = 8;
    static long STUNCOOKIE = 0x2112A442;

    static int calculateFingerprint(byte[] rawPack) {
        CRC32 crc = new CRC32();

        crc.update(rawPack, 0, rawPack.length - FPLEN);
        return (int) (crc.getValue() ^ 0x5354554eL);
    }

    static byte[] calculateMessageIntegrity(byte[] pass, ByteBuffer bb) throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
        // set the key into an HmacSHA1
        SecretKeySpec key = new SecretKeySpec(pass, "HmacSHA1");
        Mac m = Mac.getInstance("HmacSHA1");
        int tail = MILENGTH + FPLEN;
        m.init(key);
        // need to fake the length since we calculate the MI as if there is no FP
        char olen = bb.getChar(2);
        byte[] lenb = new byte[2];
        ByteBuffer bbfake = ByteBuffer.wrap(lenb);
        bbfake.putChar(0, (char) (olen - FPLEN));

        byte[] mainframe = bb.array();

        m.update(mainframe, 0, 2); // first 2 bytes
        m.update(lenb, 0, 2); // tricked up length
        m.update(mainframe, 4, mainframe.length - (tail + 4)); // everything else before the MI
        //and extract the goodies
        byte[] mi = new byte[20];
        m.doFinal(mi, 0);
        //println("calculated mi as " + Util.hexString(mi));
        return mi;
    }

    ArrayList<StunAttribute> _attributes;
    int _fingerprint;
    byte[] _messageIntegrity;
    short _mtype;

    /*
0                   1                   2                   3
0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|0 0|     STUN Message Type     |         Message Length        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         Magic Cookie                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                     Transaction ID (96 bits)                  |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    static ArrayList<StunAttribute> parseAttributes(ByteBuffer a_frame) {
        ArrayList<StunAttribute> buf = new ArrayList();
        while (a_frame.remaining() >= 4) {
            char a_type = a_frame.getChar();
            int a_len = a_frame.getChar();
            byte[] val = new byte[a_len];
            a_frame.get(val);
            ByteBuffer vbb = ByteBuffer.wrap(val);
            StunAttribute a = new StunAttribute(new Integer(a_type), a_len, vbb);
            buf.add(a);
            Log.debug("Attribute type " + a.getName() + "(" + a_type + ") " + " len " + a_len + " at " + a_frame.position());
            // now suckup any pad 
            int pad = a_len % 4;
            if (pad != 0) {
                pad = 4 - pad;
                byte[] slop = new byte[pad];
                Log.debug("padding by " + pad);
                a_frame.get(slop);
            }
        }
        return buf;
    }

    StunPacket(short mtype, int fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity) {
        _attributes = attributes;
        _fingerprint = fingerprint;
        _messageIntegrity = messageIntegrity;
        _mtype = mtype;
    }

    static StunPacket mkStunPacket(byte[] inbound, String miPass) throws Exception {
        StunPacket ret = null;
        if (inbound.length >= 20) {
            ByteBuffer b_frame = ByteBuffer.wrap(inbound);

            short mtype = b_frame.getShort();
            if ((mtype & 0xC000) == 0) {
                Log.debug("testing stun packet");
                short mlen = b_frame.getShort();
                long cookie = b_frame.getInt();
                if (cookie == STUNCOOKIE) {
                    Log.debug(" stun packet with valid cookie type = " + mtype + " length =" + mlen);
                    byte[] tid = new byte[12];
                    b_frame.get(tid);

                    if (mlen <= inbound.length - 20) {
                        ByteBuffer tit = b_frame.slice();
                        ArrayList<StunAttribute> attributes = parseAttributes(tit);
                        int fingerprint = calculateFingerprint(inbound);
                        byte[] messageIntegrity = null;
                        if (miPass != null) {
                            messageIntegrity = calculateMessageIntegrity(miPass.getBytes(), b_frame);
                        }
                        validatePacket(attributes, fingerprint, messageIntegrity);
                        switch (mtype) {
                            case 0x0101:
                                ret = new StunBindingResponse(mtype, fingerprint, attributes, messageIntegrity);
                                break;
                            case 0x0110:
                                ret = new StunErrorResponse(mtype, fingerprint, attributes, messageIntegrity);
                                break;
                            case 0x0001:
                                ret = new StunBindingRequest(mtype, fingerprint, attributes, messageIntegrity);
                                break;
                        }
                    } else {
                        throw new StunPacketException("implausible length param " + mlen + " not less than " + (inbound.length - 20));
                    }
                } else {
                    throw new StunPacketException(" invalid stun cookie");
                }

            } else {
                throw new StunPacketException("initial bits should be zero");
            }
        } else {
            throw new StunPacketException("aren't you a little short for a stun packet ?");
        }
        return ret;
    }

    private static void validatePacket(ArrayList<StunAttribute> attributes, int fingerprint, byte[] messageIntegrity) throws FingerPrintException, MessageIntegrityException {
        boolean fpOk = false;
        boolean miOk = false;

        for (StunAttribute a : attributes) {
            String name = a.getName();
            Log.debug("attribute " + name);
            if (name.equals("FINGERPRINT")) {
                Log.debug("Checking stun fingerprint");
                fpOk = (a.getInt() == fingerprint);
            }
            if (name.equals("MESSAGE-INTEGRITY")) {
                Log.debug("Checking mi");
                byte mi[] = a.getBytes();
                if (mi.length == messageIntegrity.length) {
                    int i = 0;
                    for (; i < mi.length; i++) {
                        if (mi[i] != messageIntegrity[i]) {
                            break;
                        }
                    }
                    miOk = (i == messageIntegrity.length);
                }
            }
            if (name.equals("USERNAME")) {
                Log.debug("username is: " + a.getString());
            }
        }
        if (!fpOk && !attributes.isEmpty()) {
            throw new FingerPrintException();
        }
        if (!miOk && messageIntegrity != null) { // ie we were expecting a message integrity check 
            throw new MessageIntegrityException();
        }
    }

    static class FingerPrintException extends Exception {

        public FingerPrintException() {
        }
    }

    static class MessageIntegrityException extends Exception {

        public MessageIntegrityException() {
        }
    }

    private static class StunPacketException extends Exception {

        public StunPacketException(String message) {
            super(message);
        }
    }

}

class StunErrorResponse extends StunPacket {

    public StunErrorResponse(short mtype, int fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity) {
        super(mtype, fingerprint, attributes, messageIntegrity);
    }
}

class StunBindingRequest extends StunPacket {

    public StunBindingRequest(short mtype, int fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity) {
        super(mtype, fingerprint, attributes, messageIntegrity);
    }
}

class StunBindingResponse extends StunPacket {

    public StunBindingResponse(short mtype, int fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity) {
        super(mtype, fingerprint, attributes, messageIntegrity);
    }
}
