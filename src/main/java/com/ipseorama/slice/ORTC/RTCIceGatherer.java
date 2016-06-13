/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import java.beans.EventHandler;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCIceGatherer extends RTCStatsProvider {

    private RTCIceComponent _component;

    RTCIceComponent getIceComponent() {
        return getComponent();
    }
    private RTCIceGathererState _state;

    RTCIceGathererState getState() {
        return _state;
    }

    public void close() {
    }

    public void gather(RTCIceGatherOptions options) {
    }

    public RTCIceParameters getLocalParameters() {
        return null;
    }

    List<RTCIceCandidate> getLocalCandidates() {
        return null;
    }

    RTCIceGatherer createAssociatedGatherer() {
        return null;
    }
    private EventHandler onstatechange;
    private EventHandler onerror;
    private EventHandler onlocalcandidate;

    /**
     * @return the _component
     */
    public RTCIceComponent getComponent() {
        return _component;
    }

    /**
     * @param _component the _component to set
     */
    public void setComponent(RTCIceComponent _component) {
        this._component = _component;
    }

    /**
     * @param _state the _state to set
     */
    public void setState(RTCIceGathererState _state) {
        this._state = _state;
    }

    /**
     * @return the onstatechange
     */
    public EventHandler getOnstatechange() {
        return onstatechange;
    }

    /**
     * @param onstatechange the onstatechange to set
     */
    public void setOnstatechange(EventHandler onstatechange) {
        this.onstatechange = onstatechange;
    }

    /**
     * @return the onerror
     */
    public EventHandler getOnerror() {
        return onerror;
    }

    /**
     * @param onerror the onerror to set
     */
    public void setOnerror(EventHandler onerror) {
        this.onerror = onerror;
    }

    /**
     * @return the onlocalcandidate
     */
    public EventHandler getOnlocalcandidate() {
        return onlocalcandidate;
    }

    /**
     * @param onlocalcandidate the onlocalcandidate to set
     */
    public void setOnlocalcandidate(EventHandler onlocalcandidate) {
        this.onlocalcandidate = onlocalcandidate;
    }
}
