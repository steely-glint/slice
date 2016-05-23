/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleEntry;
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
    private final Integer aType;
    private final int aLen;
    private final ByteBuffer aVal;

    String getName() {
        return findName(aType);
    }

    String getString() {
        return new String(aVal.array());
    }

    int getInt() {
        return aVal.getInt(0);
    }

    long getLong() {
        return aVal.getLong(0);
    }

    class ErrorAttribute {

        final int code;
        final String reason;

        ErrorAttribute(int c, String r) {
            code = c;
            reason = r;
        }
    };

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
        char port = aVal.getChar(2);
        byte[] addressBytes = (iptype == 1) ? new byte[4] : new byte[16];
        aVal.get(addressBytes, 4, addressBytes.length);
        InetAddress inetAd = java.net.InetAddress.getByAddress(addressBytes);
        return new InetSocketAddress(inetAd, port);
    }

    short[] getShorts() {
        short[] ret = new short[aLen / 2];
        int i = 0;
        while (i < aLen) {
            ret[i] = aVal.getShort(i);
            i += 2;
        }
        return ret;
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

    StunAttribute(Integer t, int l, ByteBuffer v) {
        aType = t;
        aLen = l;
        aVal = v;
    }
}
