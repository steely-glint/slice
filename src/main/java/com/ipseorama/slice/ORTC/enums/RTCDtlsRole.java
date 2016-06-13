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
public enum RTCDtlsRole {
    AUTO() {
        @Override
        public String toString() {
            return "auto";
        }
    },
    CLIENT() {
        @Override
        public String toString() {
            return "client";
        }
    },
    SERVER() {
        @Override
        public String toString() {
            return "server";
        }
    };
    public static RTCDtlsRole fromString(String r){
        RTCDtlsRole ret = null;
        String role = r.toLowerCase();
        switch (role){
            case "auto": ret = AUTO; break;
            case "client": ret = CLIENT; break;
            case "server": ret = SERVER; break;
        }
        return ret;
    }
}
