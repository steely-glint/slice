/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC.enums;

/**
 *
 * @author tim
 */
public enum RTCIceGatherPolicy {
    ALL() {
        @Override
        public String toString() {
            return "all";
        }
    },
    NOHOST() {
        @Override
        public String toString() {
            return "nohost";
        }
    },
    RELAY() {
        @Override
        public String toString() {
            return "relay";
        }
    };
    public static RTCIceGatherPolicy fromString(String policy){
        RTCIceGatherPolicy ret = null;
        String p = policy.toLowerCase();
        switch(p){
            case "all": ret = ALL;break;
            case "nohost": ret = NOHOST;break;
            case "relay": ret = RELAY; break;
        }
        return ret;
    }
}
