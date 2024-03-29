/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.stun.StunPacketException.FingerPrintException;
import com.ipseorama.slice.stun.StunPacketException.MessageIntegrityException;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author thp
 */
public class StunPacket {

    public static int MTU = 1409;
    static int MILENGTH = 24;
    static int FPLEN = 8;
    static long STUNCOOKIE = 0x2112A442;

    static int calculateFingerprint(byte[] rawPack) {
        CRC32 crc = new CRC32();
        crc.update(rawPack, 0, rawPack.length - FPLEN);
        return (int) (crc.getValue() ^ 0x5354554eL);
    }

    static byte[] calculateMessageIntegrity(byte[] pass, ByteBuffer bb, boolean hasfp) throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
        // set the key into an HmacSHA1
        int fplen = hasfp ? FPLEN : 0;
        SecretKeySpec key = new SecretKeySpec(pass, "HmacSHA1");
        Mac m = Mac.getInstance("HmacSHA1");
        int tail = MILENGTH + fplen;
        m.init(key);
        // need to fake the length since we calculate the MI as if there is no FP
        char olen = bb.getChar(2);
        byte[] lenb = new byte[2];
        ByteBuffer bbfake = ByteBuffer.wrap(lenb);
        bbfake.putChar(0, (char) (olen - fplen));

        byte[] mainframe = bb.array(); // for an outbound packet this will be MTU long we need to ignore the rest

        m.update(mainframe, 0, 2); // first 2 bytes
        m.update(lenb, 0, 2); // tricked up length
        m.update(mainframe, 4, (20 + olen - tail - 4)); // everything else before the MI

        //and extract the goodies
        byte[] mi = new byte[20];
        m.doFinal(mi, 0);
        //println("calculated mi as " + Util.hexString(mi));
        return mi;
    }

    static StunPacket mkStunPacket(byte[] outb, Map<String, String> miPass, InetSocketAddress near) throws Exception {
        return mkStunPacket(outb, miPass, near, null);
    }

    ArrayList<StunAttribute> _attributes;
    Integer _fingerprint;
    byte[] _messageIntegrity;
    short _mtype;
    byte[] _tid;
    byte[] _pass;
    private InetSocketAddress _far;
    private InetSocketAddress _near;
    private DatagramChannel _channel;

    public byte[] outboundBytes(Map<String, String> miPass) throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
        byte[] pass = StunPacket.findPass(_attributes, miPass, null);
        return outboundBytes(pass);
    }

    public byte[] outboundBytes() throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
        return outboundBytes(_pass);
    }

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
    public byte[] outboundBytes(byte[] pass) throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
        byte[] ret = null;
        ByteBuffer bb = ByteBuffer.allocate(MTU);
        bb.putChar((char) (0x3fff & this._mtype));
        bb.putChar((char) 0); // fix the length later....
        bb.putInt((int) STUNCOOKIE);
        bb.put(this._tid);
        int len = 20;
        if (_attributes != null) {
            len += putAttributes(bb, _attributes, pass);
        }
        ret = new byte[len];
        for (int i = 0; i < len; i++) {
            ret[i] = bb.get(i);
        }
        Log.verb("returning packet length " + len);
        return ret;
    }

    static ArrayList<StunAttribute> parseAttributes(ByteBuffer a_frame) {
        ArrayList<StunAttribute> buf = new ArrayList();
        while (a_frame.remaining() >= 4) {
            int startpos = ((Buffer) a_frame).position() + 20;
            char a_type = a_frame.getChar();
            int a_len = a_frame.getChar();
            byte[] val = new byte[a_len];
            if (val.length > 0) {
                a_frame.get(val);
            }
            ByteBuffer vbb = ByteBuffer.wrap(val);
            StunAttribute a = new StunAttribute(new Integer(a_type), a_len, vbb);
            buf.add(a);
            Log.verb("Attribute type " + a.getName() + "(" + (int) a_type + ") " + " len " + a_len + " at " + startpos);
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

    static int putAttributes(ByteBuffer bb, ArrayList<StunAttribute> attributes, byte[] pass) throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
        int len = 0;
        for (StunAttribute a : attributes) {
            Log.verb("putting attribute " + a.getName() + " at " + ((Buffer) bb).position());
            len += a.put(bb);
            bb.putChar(2, (char) len); // update the length 
            if (a.getName() != null) {
                if (a.getName().equals("MESSAGE-INTEGRITY")) {
                    byte[] mi = calculateMessageIntegrity(pass, bb, false);
                    int start = ((Buffer) bb).position() - mi.length;
                    for (int i = start; i < ((Buffer) bb).position(); i++) {
                        bb.put(i, mi[i - start]);
                    }
                }
                if (a.getName().equals("FINGERPRINT")) {
                    byte[] rawpack = new byte[((Buffer) bb).position()];
                    for (int i = 0; i < rawpack.length; i++) {
                        rawpack[i] = bb.get(i);
                    }
                    int fp = calculateFingerprint(rawpack);
                    bb.putInt(((Buffer) bb).position() - 4, fp);
                }
            }
        }
        return len;
    }

    /**
     * inbound constuctor
     */
    StunPacket(short mtype, Integer fingerprint, ArrayList<StunAttribute> attributes, byte[] messageIntegrity, InetSocketAddress near) {
        this(mtype);
        _attributes = attributes;
        _fingerprint = fingerprint;
        _messageIntegrity = messageIntegrity;
        _near = near;
    }

    /**
     * Outbound
     */
    StunPacket(short mtype) {
        _mtype = mtype;
    }

    static byte[] findPass(ArrayList<StunAttribute> attributes, Map<String, String> miPass, IceStunBindingTransaction st) throws NoSuchAlgorithmException {
        byte[] ret = null;
        String pass = null;
        String realm = null;
        String username = null;
        if ((attributes != null) && (attributes.size() > 0)) {

            for (StunAttribute a : attributes) {
                if (a.getName() != null) {
                    if (a.getName().equals("USERNAME")) {
                        username = a.getString();
                    }
                    if (a.getName().equals("REALM")) {
                        realm = a.getString();
                    }
                }
            }
            if (username == null) {
                Log.debug("no username attribute in this packet");
                if (st != null) {
                    Log.debug("looking in transaction");
                    username = st.getUserName();
                    Log.debug("found username in transaction " + username);
                }
            }
            if ((username == null) && (st == null)) {
                Optional<String> v = miPass.keySet().stream().reduce((r, k) -> {
                    return r + ":" + k;
                });
                if (v.isPresent()) {
                    username = v.get();
                    Log.debug("no transaction, so using the list we have to form username " + username);
                }
            }
            if (username != null) {
                String[] susers = username.split(":");
                int z = 0;
                pass = miPass.get(susers[0]);
                if (pass == null) {
                    pass = miPass.get(susers[1]);
                    z = 1;
                }
                if ((realm != null) && (pass != null)) {
                    pass = susers[z] + ":" + realm + ":" + pass; // should do SASLPrep
                }
                Log.verb("User =" + username + " pass =" + pass + " based on " + susers[z]);
                if (pass == null) {
                    Log.verb("miPass contained:");
                    miPass.forEach((String u, String p) -> {
                        Log.verb("\t" + u + " " + p);
                    });
                }
            } else {
                Log.debug("no username attribute in this packet or transaction");
            }
            if (pass != null) {
                ret = pass.getBytes();
                if (realm != null) {
                    MessageDigest md5;
                    md5 = MessageDigest.getInstance("MD5");
                    ret = md5.digest(ret);
                }
            } else {
                Log.debug("no matching pass found");
            }
        } else {
            Log.debug("no attributes in this packet");
        }
        return ret;
    }

    public boolean hasAttribute(String aname) {
        return _attributes == null ? false : hasAttribute(this._attributes, aname);
    }

    static boolean hasAttribute(ArrayList<StunAttribute> attributes, String aname) {
        boolean ret = false;
        for (StunAttribute a : attributes) {
            if ((a.getName() != null) && (a.getName().equals(aname))) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    StunAttribute getAttributeByName(String aname) {
        StunAttribute ret = null;
        for (StunAttribute a : this._attributes) {
            if ((a.getName() != null) && (a.getName().equals(aname))) {
                ret = a;
                break;
            }
        }
        return ret;
    }

    /*
    The idea of these static methods is basically paranoia - no stunpacket object is created untill
    the packet has passed validation checks. It also ensures we can create the correct type.
     */
    public static StunPacket mkStunPacket(byte[] inbound, Map<String, String> miPass, InetSocketAddress near, StunTransactionManager stm) throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException, StunPacketException {
        StunPacket ret = null;
        if (inbound.length >= 20) {
            ByteBuffer b_frame = ByteBuffer.wrap(inbound);

            short mtype = b_frame.getShort();
            if ((mtype & 0xC000) == 0) {
                short mlen = b_frame.getShort();
                long cookie = b_frame.getInt();
                if (cookie == STUNCOOKIE) {
                    Log.verb(" stun packet with valid cookie type = " + mtype + " length =" + mlen);
                    byte[] tid = new byte[12];
                    b_frame.get(tid);
                    IceStunBindingTransaction trans = stm.getIceBindingTrans(tid);

                    if (mlen <= inbound.length - 20) {
                        ByteBuffer tit = b_frame.slice();
                        ArrayList<StunAttribute> attributes = parseAttributes(tit);
                        byte[] pass = findPass(attributes, miPass, trans);
                        Integer fingerprint = null;
                        if (hasAttribute(attributes, "FINGERPRINT")) {
                            fingerprint = new Integer(calculateFingerprint(inbound));
                        }
                        byte[] messageIntegrity = null;
                        if (hasAttribute(attributes, "MESSAGE-INTEGRITY")) {
                            if (pass != null) {
                                messageIntegrity = calculateMessageIntegrity(pass, b_frame, fingerprint != null);
                            } else {
                                if (Log.getLevel() >= Log.DEBUG) {
                                    Log.debug("type = " + mtype);
                                    Log.debug("Tid = " + hexString(tid));
                                    if (attributes != null) {
                                        Log.debug("Attributes");
                                        attributes.stream().forEach((StunAttribute a) -> {
                                            Log.debug("\t" + a.toString(tid));
                                        });
                                    }
                                }
                                //throw new MessageIntegrityException("No pass available...", ret);
                            }
                        }
                        try {
                            switch (mtype) {
                                case 0x0101:
                                    ret = new StunBindingResponse(mtype, fingerprint, attributes, messageIntegrity, near);
                                    break;
                                case 0x0110:
                                    ret = new StunErrorResponse(mtype, fingerprint, attributes, messageIntegrity, near);
                                    break;
                                case 0x0001:
                                    ret = new StunBindingRequest(mtype, fingerprint, attributes, messageIntegrity, near);
                                    break;
                                case 0x0010:
                                    ret = new StunIndication(mtype, fingerprint, attributes, messageIntegrity, near);
                                    break;
                                default:
                                    ret = new StunPacket(mtype, fingerprint, attributes, messageIntegrity, near);
                                    break;
                            }
                            if (ret != null) {
                                ret.setTid(tid);
                                ret.setPass(pass);
                            }
                            // I'd _really_ rather validate this packet before we parse it - but 
                            // then the exception doesn't have a packet for debug...
                            validatePacket(attributes, fingerprint, messageIntegrity);
                        } catch (MessageIntegrityException mex) {
                            mex.setPacket(ret);
                            ret = null;
                            throw (mex);
                        }

                    } else {
                        throw new StunPacketException("implausible length param " + mlen + " not less than " + (inbound.length - 20), ret);
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

    private static void validatePacket(ArrayList<StunAttribute> attributes, Integer fingerprint, byte[] messageIntegrity) throws FingerPrintException, MessageIntegrityException {
        boolean fpOk = false;
        boolean miOk = false;
        byte mi[] = {};
        for (StunAttribute a : attributes) {
            String name = a.getName();
            Log.verb("attribute " + name);
            if (name != null) {
                if (name.equals("FINGERPRINT")) {
                    Log.verb("Checking stun fingerprint");
                    fpOk = (a.getInt() == fingerprint);
                }
                if (name.equals("MESSAGE-INTEGRITY") && (messageIntegrity != null)) {
                    Log.verb("Checking mi");
                    mi = a.getBytes();
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
                    Log.verb("username is: " + a.getString());
                }
            }
        }
        if (!fpOk && fingerprint != null) {
            throw new FingerPrintException();
        }
        if (!miOk && messageIntegrity != null) { // ie we were expecting a message integrity check 
            if (Log.getLevel() >= Log.DEBUG) {
                StringBuffer a = new StringBuffer();
                for (byte m : mi) {
                    if (((0xff)&m) < 16) a.append("0");
                    a.append(Integer.toHexString((0xff) & m));
                }
                StringBuffer b = new StringBuffer();
                for (byte m : messageIntegrity) {
                    if (((0xff)&m) < 16) a.append("0");
                    b.append(Integer.toHexString((0xff) & m));
                }
                Log.debug("Message integrity mismatch:");
                Log.debug("\t"+a);
                Log.debug("\t"+b);

            }
            throw new MessageIntegrityException("Expected a MessageIntegrity but it didn't checkout...");
        }
    }

    void setTid(byte[] tid) {
        _tid = tid;
    }

    public byte[] getTid() {
        return _tid;
    }

    byte[] getPass() {
        return _pass;
    }

    private void setPass(byte[] pass) {
        _pass = pass;
    }

    void setAttributes(ArrayList<StunAttribute> attrs) {
        _attributes = attrs;
    }

    /**
     * @return the _far
     */
    public InetSocketAddress getFar() {
        return _far;
    }

    /**
     * @param _far the far Address to set
     */
    public void setFar(InetSocketAddress far) {
        if (far.isUnresolved()) {
            String fs = far.getHostName();
            Log.debug("tried to resolve " + fs);
        }
        if (far.isUnresolved()) {
            Log.debug("failed to resolve " + far);
        } else {
            this._far = far;
        }
    }

    /**
     * @return the _near
     */
    public InetSocketAddress getNear() {
        return _near;
    }

    /**
     * @param _near the _near to set
     */
    public void setNear(InetSocketAddress _near) {
        this._near = _near;
    }

    public void setChannel(DatagramChannel dgc) {
        this._channel = dgc;
    }

    public DatagramChannel getChannel() {
        return this._channel;
    }

    public static String hexString(byte[] buf) {
        StringBuilder b = new StringBuilder();

        for (byte r : buf) {
            String dig = Integer.toHexString((0xff) & r).toUpperCase();
            if (dig.length() == 1) {
                b.append('0');
            }
            b.append(dig);
        }
        return b.toString();
    }

    public String toString() {
        return "Stun pkt " + this.getClass().getSimpleName() + " far =" + this._far + " near =" + this._near;
    }

    public String listAttribs() {
        StringBuffer retB = new StringBuffer();
        this._attributes.forEach((a) -> {
            retB.append(a.toString(_tid)).append(" ");
        });
        return retB.toString();
    }
}
