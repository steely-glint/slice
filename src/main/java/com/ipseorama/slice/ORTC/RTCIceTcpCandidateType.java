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
public enum RTCIceTcpCandidateType {
    ACTIVE() {
        @Override
        public String toString() {
            return "active";
        }
    },
    PASSIVE() {
        @Override
        public String toString() {

            return "passive";
        }
    },
    SO() {
        @Override
        public String toString() {

            return "so";
        }
    };

    public static RTCIceTcpCandidateType fromString(String p) {
        RTCIceTcpCandidateType ret = null;
        String protocol = p.toLowerCase();
        switch (protocol) {
            case "active":
                ret = ACTIVE;
                break;
            case "passive":
                ret = PASSIVE;
                break;
            case "so":
                ret = SO;
                break;
        }
        return ret;
    }
}
