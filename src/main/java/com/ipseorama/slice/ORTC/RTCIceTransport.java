/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceTransportState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.phono.srtplight.Log;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tim
 */
public class RTCIceTransport {

    RTCIceGatherer iceGatherer;
    RTCIceRole role;
    final RTCIceComponent component;
    protected RTCIceTransportState state;
    RTCIceParameters remoteParameters;
    List<RTCIceCandidate> remoteCandidates;
    List<RTCIceCandidatePair> candidatePairs;

    public RTCIceTransportState getRTCIceTransportState() {
        return state;
    }

    public List<RTCIceCandidate> getRemoteCandidates() {
        return remoteCandidates;
    }

    public RTCIceCandidatePair getSelectedCandidatePair() {
        return null;
    }

    public void start(RTCIceGatherer gatherer, RTCIceParameters remoteParameters, RTCIceRole role) {
        // for the moment we will ignore the new values and assume that the constructor was right....
        // and ignore the re-start semantics.
        gatherer.onlocalcandidate = (RTCEventData c) -> {
            if (c instanceof RTCIceCandidate) {
                RTCIceCandidate l = (RTCIceCandidate) c;
                List<RTCIceCandidate> remotes = new ArrayList(remoteCandidates);
                for (RTCIceCandidate r : remotes) {
                    addPair(l, r);
                }
            }
        };

    }

    public void stop() {
    }

    public RTCIceParameters getRemoteParameters() {
        return remoteParameters;
    }

    RTCIceTransport createAssociatedTransport() {
        return null; // don't do this - rtcp-mux is good.
    }

    public void addRemoteCandidate(RTCIceGatherCandidate remoteCandidate) {
        if (remoteCandidate instanceof RTCIceCandidate) {
            RTCIceCandidate r = (RTCIceCandidate) remoteCandidate;
            List<RTCIceCandidate> locals = new ArrayList(iceGatherer.getLocalCandidates());
            for (RTCIceCandidate l : locals) {
                addPair(l, r);
            }
        } else {
            // assume end-of-candidates....
        }
    }

    public void setRemoteCandidates(List<RTCIceCandidate> remoteCandidates) {
        // assumption is that this blatts the existing list - so we need to
        // compare and selectively add/delete - I suppose?!?
        // ignore for now.
        // assume all candidates are added vi addRemoteCandidate ;-)
    }

    EventHandler onstatechange;
    EventHandler oncandidatepairchange;

    public RTCIceTransport(RTCIceGatherer ig,
            RTCIceRole r,
            RTCIceComponent comp) {
        this.iceGatherer = ig;
        this.role = r;
        this.component = comp;
        this.state = state.NEW;
        this.remoteCandidates = new ArrayList();
    }

    /**
     * @param _state the _state to set
     */
    private void setState(RTCIceTransportState newstate) {
        this.state = newstate;
        if (this.onstatechange != null) {
            onstatechange.onEvent(newstate);
        }
    }

    private void addPair(RTCIceCandidate l, RTCIceCandidate r) {
        if (l.getProtocol() == r.getProtocol() && l.getIpVersion() == r.getIpVersion())  {
            synchronized (candidatePairs) {
                boolean present = candidatePairs.stream().anyMatch((RTCIceCandidatePair p) -> {
                    return p.getLocal().equals(l) && p.getRemote().equals(r);
                });
                if (!present) {
                    RTCIceCandidatePair p = new RTCIceCandidatePair(l, r);
                    if (p != null) {
                        candidatePairs.add(p);
                        Log.debug("added candidate pair "+p.toString());
                    }
                } else {
                    Log.debug("ignoring exisiting candidate pair "+r.toString()+" "+l.toString());
                }
            }
        } else {
            Log.debug("ignoring incompatiple candidate pair "+r.toString()+" "+l.toString());
        }
    }
}
