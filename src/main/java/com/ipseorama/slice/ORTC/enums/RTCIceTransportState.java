/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC.enums;

import com.ipseorama.slice.ORTC.RTCEventData;

/**
 *
 * @author tim
 */
public enum RTCIceTransportState implements RTCEventData{
    NEW() {
        @Override
        public String toString() {
            return "new";
        }
    },
    CHECKING() {
        @Override
        public String toString() {
            return "checking";
        }
    },
    CONNECTED() {
        @Override
        public String toString() {
            return "connected";
        }
    },
    COMPLETED() {
        @Override
        public String toString() {
            return "completed";
        }
    },
    DISCONNECTED() {
        @Override
        public String toString() {
            return "disconnected";
        }
    },
    FAILED() {
        @Override
        public String toString() {
            return "failed";
        }
    },
    CLOSED() {
        @Override
        public String toString() {
            return "closed";
        }
    }
};
