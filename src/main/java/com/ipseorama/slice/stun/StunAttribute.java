/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author thp
 */
public class StunAttribute {

    final static Map<Integer, String> __typeMap
            = Collections.unmodifiableMap(Stream.of(
                    //0x0000->"Reserved,[RFC5389]
                    new SimpleEntry<>(0x0001, "MAPPED-ADDRESS"), //[RFC5389]
                    //0x0002->"Reserved; was RESPONSE-ADDRESS,[RFC5389]
                    new SimpleEntry<>(0x0003, "CHANGE-REQUEST"), //[RFC5780]
                    //0x0004->"Reserved; was SOURCE-ADDRESS,[RFC5389]
                    //0x0005->"Reserved; was CHANGED-ADDRESS,[RFC5389]
                    new SimpleEntry<>(0x0006, "USERNAME"), //[RFC5389]
                    //0x0007->"Reserved; was PASSWORD,[RFC5389]
                    new SimpleEntry<>(0x0008, "MESSAGE-INTEGRITY"), //[RFC5389]
                    new SimpleEntry<>(0x0009, "ERROR-CODE"), //[RFC5389]
                    new SimpleEntry<>(0x000A, "UNKNOWN-ATTRIBUTES"), //[RFC5389]
                    //0x000B->"Reserved; was REFLECTED-FROM,[RFC5389]
                    new SimpleEntry<>(0x000C, "CHANNEL-NUMBER"), //[RFC5766]
                    new SimpleEntry<>(0x000D, "LIFETIME"), //[RFC5766]
                    //0x000E-0x000F,Reserved,
                    //0x0010,Reserved (was BANDWIDTH),[RFC5766]
                    //0x0011,Reserved,
                    new SimpleEntry<>(0x0012, "XOR-PEER-ADDRESS"), //[RFC5766]
                    new SimpleEntry<>(0x0013, "DATA"), //[RFC5766]
                    new SimpleEntry<>(0x0014, "REALM"), //[RFC5389]
                    new SimpleEntry<>(0x0015, "NONCE"), //[RFC5389]
                    new SimpleEntry<>(0x0016, "XOR-RELAYED-ADDRESS"), //[RFC5766]
                    new SimpleEntry<>(0x0017, "REQUESTED-ADDRESS-FAMILY"), //[RFC6156]
                    new SimpleEntry<>(0x0018, "EVEN-PORT"), //[RFC5766]
                    new SimpleEntry<>(0x0019, "REQUESTED-TRANSPORT"), //[RFC5766]
                    new SimpleEntry<>(0x001A, "DONT-FRAGMENT"), //[RFC5766]
                    //0x001B-0x001F,Unassigned,
                    new SimpleEntry<>(0x0020, "XOR-MAPPED-ADDRESS"), //[RFC5389]
                    //0x0021,Reserved (was TIMER-VAL),[RFC5766]
                    new SimpleEntry<>(0x0022, "RESERVATION-TOKEN"), //[RFC5766]
                    //0x0023,Reserved,
                    new SimpleEntry<>(0x0024, "PRIORITY"), //[RFC5245]
                    new SimpleEntry<>(0x0025, "USE-CANDIDATE"), //[RFC5245]
                    new SimpleEntry<>(0x0026, "PADDING"), //[RFC5780]
                    new SimpleEntry<>(0x0027, "RESPONSE-PORT"), //[RFC5780]
                    //0x0028-0x0029,Reserved,
                    new SimpleEntry<>(0x002A, "CONNECTION-ID"), //[RFC6062]
                    //0x002B-0x002F,Unassigned,
                    //0x0030->"Reserved,
                    //0x0031-0x7FFF,Unassigned,
                    //0x8000-0x8021,Unassigned,
                    new SimpleEntry<>(0x8022, "SOFTWARE"), //[RFC5389]
                    new SimpleEntry<>(0x8023, "ALTERNATE-SERVER"), //[RFC5389]
                    //0x8024->"Reserved,
                    //0x8025->"Unassigned,
                    //0x8026,Reserved,
                    new SimpleEntry<>(0x8027, "CACHE-TIMEOUT"), //[RFC5780]
                    new SimpleEntry<>(0x8028, "FINGERPRINT"), //[RFC5389]
                    new SimpleEntry<>(0x8029, "ICE-CONTROLLED"), //[RFC5245]
                    new SimpleEntry<>(0x802A, "ICE-CONTROLLING"), //[RFC5245]
                    new SimpleEntry<>(0x802B, "RESPONSE-ORIGIN"), //[RFC5780]
                    new SimpleEntry<>(0x802C, "OTHER-ADDRESS"), //[RFC5780]
                    new SimpleEntry<>(0x802D, "ECN-CHECK STUN"), //[RFC6679]
                    //0x802E-0xBFFF,Unassigned,
                    new SimpleEntry<>(0xC000, "CISCO-STUN-FLOWDATA" //[Dan_Wing]
                    //0xC001-0xFFFF,Unassigned,
                    )
            ).collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
    final static Map<String, Integer> __nameMap = __typeMap.entrySet().stream().collect(Collectors.toMap(
            e -> e.getValue(),
            e -> e.getKey()));

    static String findName(Integer typeV) {
        return __typeMap.get(typeV);
    }

    static Integer findType(String name) {
        return __nameMap.get(name);
    }

    static void addPriority(ArrayList<StunAttribute> attrs, int reflexPri) {
        StunAttribute p = new StunAttribute("PRIORITY");
        p.setInt(reflexPri);
        attrs.add(p);
    }

    static void addIceCon(ArrayList<StunAttribute> attrs, RTCIceRole role, long tb) {
        StunAttribute r = new StunAttribute("ICE-" + role.toString().toUpperCase());
        r.setLong(tb);
        attrs.add(r);
    }

    static void addFingerprint(ArrayList<StunAttribute> attrs) {
        StunAttribute f = new StunAttribute("FINGERPRINT");
        f.setInt(0);
        attrs.add(f);
    }

    static void addMessageIntegrity(ArrayList<StunAttribute> attrs) {
        StunAttribute mi = new StunAttribute("MESSAGE-INTEGRITY");
        mi.setBytes(new byte[20]);
        attrs.add(mi);
    }

    static void addUsername(ArrayList<StunAttribute> attrs, String outboundUser) {
        StunAttribute u = new StunAttribute("USERNAME");
        u.setString(outboundUser);
        attrs.add(u);
    }

    static void addSoftware(ArrayList<StunAttribute> attrs) {
        StunAttribute s = new StunAttribute("SOFTWARE");
        s.setString("pe.pi.slice.ORTC");
        attrs.add(s);
    }

    static void addUseCandidate(ArrayList<StunAttribute> attrs) {
        StunAttribute s = new StunAttribute("USE-CANDIDATE");
        attrs.add(s);
    }

    private final Integer aType;
    private int aLen;
    private ByteBuffer aVal;
    private boolean _xor;

    String getName() {
        return findName(aType);
    }

    void setString(String s) {
        aLen = s.length();
        aVal = ByteBuffer.allocate(aLen);
        aVal.put(s.getBytes());
    }

    String getString() {
        byte b[] = new byte[aLen];
        for (int i = 0; i < aLen; i++) {
            b[i] = aVal.get(i);
        }
        return new String(b);
    }

    int getInt() {
        return aVal.getInt(0);
    }

    void setInt(int n) {
        aLen = 4;
        aVal = ByteBuffer.allocate(aLen);
        aVal.putInt(n);
    }

    long getLong() {
        return aVal.getLong(0);
    }

    void setLong(long n) {
        aLen = 8;
        aVal = ByteBuffer.allocate(aLen);
        aVal.putLong(n);
    }

    class ErrorAttribute {

        final int code;
        final String reason;

        ErrorAttribute(int c, String r) {
            code = c;
            reason = r;
        }
    };

    void setError(ErrorAttribute ea) {
        aLen = 4 + ea.reason.length();
        aVal = ByteBuffer.allocate(aLen);
        aVal.putInt(ea.code);
        aVal.put(ea.reason.getBytes());
    }

    ErrorAttribute getError() {
        short code = aVal.getShort(2);
        byte[] reasonBytes = new byte[aLen - 4];
        aVal.get(reasonBytes, 4, aLen - 4);
        String reason = new String(reasonBytes);
        return new ErrorAttribute(code, reason);

    }

    // note to self - never use the stateful get() modes of byteBuffer here -
    // always use indexes.
    InetSocketAddress getIpAddress() throws UnknownHostException {
        byte iptype = aVal.get(1);
        int port = (0xffff) & aVal.getChar(2);
        byte[] addressBytes = (iptype == 1) ? new byte[4] : new byte[16];
        for (int i = 0; i < addressBytes.length; i++) {
            addressBytes[i] = aVal.get(4 + i);
        }
        InetAddress inetAd = java.net.InetAddress.getByAddress(addressBytes);
        return new InetSocketAddress(inetAd, port);
    }

    InetSocketAddress getXorIpAddress(byte[] tid) throws UnknownHostException {
        // RFC assumes you'll be pointer diddling in C - so this is ugly.
        // we need to xor the v6 address with bytes 4->20 of the packet.
        // which encompasses the cookie and the TID concatenated.
        // cookie is already there

        byte iptype = aVal.get(1);
        char port = (char) ((0xffff) & aVal.getChar(2));

        // port is always xor'd with the COOKIE
        int xport = (port ^ (char) (StunPacket.STUNCOOKIE >> 16));

        // so xor the address bytes with the current packet content.
        int addresslen = (iptype == 1) ? 4 : 16;
        ByteBuffer xb = ByteBuffer.allocate(addresslen);

        xb.putInt((int) StunPacket.STUNCOOKIE);
        if (addresslen == 16) {
            xb.put(tid); // for v6 we need the TID in this buffer.
        }
        byte[] addressBytes = (iptype == 1) ? new byte[4] : new byte[16];

        for (int i = 0; i < addressBytes.length; i++) {
            addressBytes[i] = (byte) (aVal.get(4 + i) ^ xb.get(i));
        }
        InetAddress inetAd = java.net.InetAddress.getByAddress(addressBytes);
        return new InetSocketAddress(inetAd, xport);
    }

    void setIpAddress(InetSocketAddress addr) {
        InetAddress ipa = addr.getAddress();
        byte code = 1;
        int len = 4;
        if (ipa instanceof java.net.Inet6Address) {
            code = 2;
            len += 12;
        }
        aLen = 4 + len;
        aVal = ByteBuffer.allocate(aLen);
        aVal.put((byte) 0);
        aVal.put(code);
        aVal.putShort((short) addr.getPort());
        byte[] iad = ipa.getAddress();
        aVal.put(iad);
    }

    void setXorIpAddress(InetSocketAddress addr) {
        setIpAddress(addr);
        _xor = true;
    }

    short[] getShorts() {
        short[] ret = new short[aLen / 2];
        int i = 0;
        while (i < aLen) {
            ret[i >> 1] = aVal.getShort(i);
            i += 2;
        }
        return ret;
    }

    void setShorts(short[] v) {
        aLen = v.length * 2;
        aVal = ByteBuffer.allocate(aLen);
        for (short s : v) {
            aVal.putShort(s);
        }
    }

    byte[] getBytes() {
        byte[] ret = new byte[aLen];
        int i = 0;
        while (i < aLen) {
            ret[i] = aVal.get(i);
            i++;
        }
        return ret;
    }

    void setBytes(byte[] v) {
        aLen = v.length;
        aVal = ByteBuffer.allocate(aLen);
        aVal.put(v);
    }

    /**
     * inbound constructor
     *
     * @param t
     * @param l
     * @param v
     */
    StunAttribute(Integer t, int l, ByteBuffer v) {
        aType = t;
        aLen = l;
        aVal = v;
    }

    /**
     * outbound constructor
     *
     * @param t
     */
    StunAttribute(String t) {
        aType = findType(t);
    }

    /**
     *
     * @param out ByteBuffer for whole packet. Correct operation of xor requires
     * that it starts at the packet head.
     * @return number of bytes added (inc padding)
     */
    int put(ByteBuffer out) {
        int len;
        out.putChar((char) ((0xffff) & this.aType));
        out.putShort((short) aLen);
        if (_xor && aVal != null) {
            out.put(aVal.get(0));
            out.put(aVal.get(1));
            out.put((byte) (aVal.get(2) ^ out.get(4)));
            out.put((byte) (aVal.get(3) ^ out.get(5)));
            for (int i = 4; i < aLen; i++) {
                byte xv = (byte) (aVal.get(i) ^ out.get(i));
                out.put(xv);
            }
        } else {
            for (int i = 0; i < aLen; i++) {
                out.put(aVal.get(i));
            }
        }
        // and add any padding too.
        len = aLen;
        int remain = (len % 4) == 0 ? 0 : 4 - (len % 4);
        for (int i = 0; i < remain; i++) {
            out.put((byte) 0);
        }
        return 4 + len + remain;
    }

    public String toString(byte[] tid)  {
        StringBuffer ret = new StringBuffer(this.getName());
        switch (this.aType) {
            case 0x0001:
                try {
                    ret.append("=").append(this.getIpAddress());
                } catch (Exception z) {
                    ret.append(z.getClass().getSimpleName());
                }
                break;
            case 0x0006:
                ret.append("=").append(this.getString());
                break;
            case 0x0012:
            case 0x0016:
            case 0x0020:
                try {
                    ret.append("=").append(this.getXorIpAddress(tid));
                } catch (Exception z) {
                    ret.append(z.getClass().getSimpleName());
                }
                break;
            default:
                ret.append("=").append(StunPacket.hexString(this.getBytes()));
                break;
        }
        return ret.toString();
    }
}
