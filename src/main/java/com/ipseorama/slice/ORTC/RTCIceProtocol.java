/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

/**
 *
 * @author tim
 */
public enum RTCIceProtocol {
    UDP() {
        @Override
        public String toString() {
            return "udp";
        }
    },
    TCP() {
        @Override
        public String toString() {
            return "tcp";
        }
    };

   public static RTCIceProtocol fromString(String p) {
        RTCIceProtocol ret = null;
        String protocol = p.toLowerCase();
        switch (protocol) {
            case "tcp":
                ret = TCP;
                break;
            case "udp":
                ret = UDP;
                break;
        }
        return ret;
    }

}
