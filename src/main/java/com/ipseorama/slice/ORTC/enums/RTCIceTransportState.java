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
    
    /*
        The RTCIceTransport is currently gathering local candidates, or is waiting for the remote device to begin to transmit
        the remote candidates, or both. In this state, checking of candidates to look for those which might be acceptable has
        not yet begun.
        */
    NEW() {
        @Override
        public String toString() {
            return "new";
        }
    },
        /*
        At least one remote candidate has been received, and the RTCIceTransport has begun examining pairings of 
        remote and local candidates in order to attempt to identify viable pairs that could be used to establish 
        a connection. Keep in mind that gathering of local candidates may still be underway, and, similarly, 
        the remote device also may still be gathering candidates of its own.

        */
    CHECKING() {
        @Override
        public String toString() {
            return "checking";
        }
    },
        /*
        A viable candidate pair has been found and selected, and the RTCIceTransport has connected the two peers together
        using that pair. However, there are still candidates pairings to consider, and there may still be gathering underway
        on one or both of the two devices.
        */
    CONNECTED() {
        @Override
        public String toString() {
            return "connected";
        }
    },
        /*
        The transport has finished gathering local candidates and has received a notification from the remote peer 
        that no more candidates will be sent. In addition, all candidate pairs have been considered and a pair has 
        been selected for use. If consent checks fail on all successful candidate pairs, the transport state will change to "failed".
        */
    COMPLETED() {
        @Override
        public String toString() {
            return "completed";
        }
    },
        /*
        The ICE agent has determined that connectivity has been lost for this RTCIceTransport. 
        This is not a failure state (that's "failed"). A value of "disconnected" means that a transient issue 
        has occurred that has broken the connection, but that should resolve itself automatically without 
        your code having to take any action. See The disconnected state for additional details.
        */
    DISCONNECTED() {
        @Override
        public String toString() {
            return "disconnected";
        }
    },
        /*
        The RTCIceTransport has finished the gathering process, has received the "no more candidates"
        notification from the remote peer, and has finished checking pairs of candidates, without successfully 
        finding a pair that is both valid and for which consent can be obtained. 
        This is a terminal state, indicating that the connection cannot be achieved or maintained.

        */
    FAILED() {
        @Override
        public String toString() {
            return "failed";
        }
    },
        /*
        The transport has shut down and is no longer responding to STUN requests.
        */
    CLOSED() {
        @Override
        public String toString() {
            return "closed";
        }
    }
};
