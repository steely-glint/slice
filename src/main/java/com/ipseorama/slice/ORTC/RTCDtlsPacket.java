/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.phono.srtplight.Log;
import java.util.Arrays;
import java.util.EnumMap;

/**
 *
 * @author thp
 */
public class RTCDtlsPacket implements RTCEventData {

    public byte[] data;

    final static int HelloRequest = 0x0;
    final static int ClientHello = 0x01;
    final static int ServerHello = 0x02;
    final static int Certificate = 0x0B;
    final static int ServerKeyExchange = 0x0C;
    final static int CertificateRequest = 0x0D;
    final static int ServerHelloDone = 0x0E;
    final static int CertificateVerify = 0x0F;
    final static int ClientKeyExchange = 0x10;
    final static int Finished = 0x14;

    final static int[] serverfinals = {Finished, ServerHelloDone};
    final static int[] clientfinals = {Finished, ClientHello};

    public static boolean isEndOfFlight(boolean server, byte[] data) {
        boolean ret = true; // default assumption
        int contentType = data[0] & 0xff;

        if (contentType == 22) { 
            int handshakeType = data[13] & 0xff;
            Log.verb("Checking DTLS handshake packet for sped "+getName(handshakeType));

            int[] finals = server ? serverfinals : clientfinals;
            ret = Arrays.stream(finals).filter((a) -> a == handshakeType).count() > 0;
        }
        Log.verb("sped DTLS Isendofflight returning "+ret);
        return ret;
    }
    
    public static String getName(int shake){
        return switch(shake) {
            case HelloRequest -> "HelloRequest";
            case ClientHello ->"ClientHello";
            case ServerHello ->"ServerHello";
            case Certificate -> "Certificate";
            case ServerKeyExchange -> "ServerKeyExchange";
            case CertificateRequest -> "CertificateRequest";
            case ServerHelloDone -> "ServerHelloDone";
            case CertificateVerify-> "CertificateVerify";
            case ClientKeyExchange ->"ClientKeyExchange";
            case Finished ->"Finished";
            default -> "Unknown("+shake+")";
        };
    }

}
