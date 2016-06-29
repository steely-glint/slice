/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;

/**
 *
 * @author thp
 */
@FunctionalInterface
public interface EventHandler {
    public void onEvent(RTCEventData data);
}
