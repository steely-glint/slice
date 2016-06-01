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
enum RTCDtlsTransportState {
    NEW() {
        @Override
        public String toString() {
            return "new";
        }
    },
    CONNECTING() {
        @Override
        public String toString() {
            return "connecting";
        }
    },
    CONNECTED() {
        @Override
        public String toString() {
            return "connected";
        }
    },
    CLOSED() {
        @Override
        public String toString() {
            return "closed";
        }
    },
    FAILED() {
        @Override
        public String toString() {
            return "failed";
        }
    }
};
