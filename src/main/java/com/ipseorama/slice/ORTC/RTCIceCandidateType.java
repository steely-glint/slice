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
enum RTCIceCandidateType {
    HOST() {
        @Override
        public String toString() {
            return "host";
        }
    },
    SRFLX() {
        @Override
        public String toString() {
            return "srflx";
        }
    },
    PRFLX() {
        public String toString() {
            return "prflx";
        }
    },
    RELAY() {
        public String toString() {
            return "relay";
        }
    };
    public static RTCIceCandidateType fromString(String t) {
        RTCIceCandidateType ret = null;
        String ty = t.toLowerCase();
        switch (ty) {
            case "host": ret = HOST;break;
            case "srflx": ret = SRFLX; break;
            case "prflx": ret = PRFLX; break;
            case "relay": ret = RELAY;break;
        }
        return ret;
    }
}
