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
public enum RTCIceCandidatePairState implements RTCEventData {

    /*
        A check has not been performed for this pair, and can be performed as soon as it is the highest-priority Waiting pair on the check list.
     */
    WAITING() {
        @Override
        public String toString() {
            return "waiting";
        }
    },
    /*
        A check has been sent for this pair, but the transaction is in progress.
     */
    INPROGRESS() {
        @Override
        public String toString() {
            return "in-progress";
        }
    },
    /*
        A check for this pair was already done and produced a successful result.
     */
    SUCCEEDED() {
        @Override
        public String toString() {
            return "succeeded";
        }
    },
    /*
        This pair has been nominated by the ICE COMTROLLING agent.
    */
    NOMINATED() {
        @Override
        public String toString() {
            return "nominated";
        }
    },
    /*
        A check for this pair was already done and failed, either never producing any response or producing an unrecoverable failure response.
     */
    FAILED() {
        @Override
        public String toString() {
            return "failed";
        }
    },
    /*
        A check for this pair hasn't been performed, and it can't yet be performed until some other check succeeds, allowing this pair to unfreeze and move into the Waiting state.
     */
    FROZEN() {
        @Override
        public String toString() {
            return "frozen";
        }
    };

}
