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
public enum RTCIceCandidatePairState {
    WAITING() {
        @Override
        public String toString() {
            return "waiting";
        }
    },
    INPROGRESS() {
        @Override
        public String toString() {
            return "in-progress";
        }
    },
    SUCCEEDED() {
        @Override
        public String toString() {
            return "succeeded";
        }
    },
    FAILED() {
        @Override
        public String toString() {
            return "failed";
        }
    },
    FROZEN() {
        @Override
        public String toString() {
            return "frozen";
        }
    };

}
