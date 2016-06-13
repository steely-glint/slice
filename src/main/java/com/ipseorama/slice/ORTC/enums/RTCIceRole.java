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
public enum RTCIceRole {
    CONTROLLING() {
        @Override
        public String toString() {
            return "controlling";
        }
    },
    CONTROLLED() {
        @Override
        public String toString() {
            return "controlled";
        }
    };
    public static RTCIceRole fromString(String s){
        RTCIceRole ret = null;
        String sl = s.toLowerCase();
        switch(sl){
            case "controlling": ret = CONTROLLING;break;
            case "controlled": ret = CONTROLLED; break;
        }
        return ret;
    }
}
