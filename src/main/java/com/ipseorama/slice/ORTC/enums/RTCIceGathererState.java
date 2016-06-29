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
public enum RTCIceGathererState implements RTCEventData{
    NEW() {
        @Override
        public String toString() {
            return "new";
        }
    },
    GATHERING() {
        @Override
        public String toString() {
            return "gathering";
        }
    },
    COMPLETE() {
        @Override
        public String toString() {
            return "complete";
        }
    },
    CLOSED() {
        @Override
        public String toString() {
            return "closed";
        }
    };
}
