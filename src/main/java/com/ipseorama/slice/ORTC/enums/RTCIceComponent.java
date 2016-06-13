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
public enum RTCIceComponent {
    RTP() {
        @Override
        public String toString() {
            return "RTP";
        }
    },
    RTCP() {
        @Override
        public String toString() {
            return "RTCP";
        }
    }
};
