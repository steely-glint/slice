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
enum RTCIceCredentialType {
    PASSWORD() {
        @Override
        public String toString() {
            return "password";
        }
    },
    TOKEN() {
        @Override
        public String toString() {
            return "token";
        }
    };

    static RTCIceCredentialType fromString(String ty) {
        RTCIceCredentialType ret = null;
        String t = ty.toLowerCase();
        switch (t) {
            case "password":
                ret = PASSWORD;
                break;
            case "token":
                ret = TOKEN;
                break;
        }
        return ret;
    }
}
