/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCIceCandidate;
import com.ipseorama.slice.ORTC.RTCIceGatherOptions;
import com.ipseorama.slice.ORTC.RTCIceGatherer;
import com.ipseorama.slice.ORTC.RTCIceParameters;
import com.ipseorama.slice.ORTC.RTCIceServer;
import com.ipseorama.slice.ORTC.RTCIceTransport;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceCredentialType;
import com.ipseorama.slice.ORTC.enums.RTCIceGatherPolicy;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.phono.srtplight.Log;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 *
 * @author tim
 */
public class Slice {

    RTCIceTransport ice;
    private final SecureRandom random;
    private final String lufrag;
    private final String lupass;
    private final RTCIceTransport transport;

    public Slice() {
        random = new SecureRandom();
        lufrag = Long.toString(random.nextLong(), 36).replace("-", "");
        lupass = (new BigInteger(130, random).toString(32)).replace("-", "");
        String rufrag = lufrag;
        String rpass = lupass;
        // hack - we will use the same upass and ufrag at both ends :-)

        RTCIceGatherer gatherer = new RTCIceGatherer();

        SingleThreadNioIceEngine tie = new SingleThreadNioIceEngine();
        tie.addIceCreds(lufrag, lupass);
        RTCIceParameters localParameters = new RTCIceParameters(lufrag, lupass, false);
        gatherer.setLocalParameters(localParameters);
        gatherer.setIceEngine(tie);
        RTCIceRole icerole = RTCIceRole.CONTROLLED;
        transport = new RTCIceTransport(gatherer, icerole, RTCIceComponent.RTP);
        gatherer.onstatechange = (RTCEventData d) -> {
            Log.debug("gatherer state is now " + d.toString());
            if (gatherer.getState() == RTCIceGathererState.GATHERING) {
                RTCIceParameters remoteParameters = new RTCIceParameters(rufrag, rpass, false);
                Log.debug("starting transport as " + icerole);
                transport.start(gatherer, remoteParameters, icerole);
            }
        };
        transport.onstatechange = (RTCEventData d) -> {
            Log.info("transport state is now " + d.toString());
        };
        transport.oncandidatepairchange = (RTCEventData d) -> {
            Log.info("selected pair is now " + d);
        };
        gatherer.onlocalcandidate = (RTCEventData d) -> {
            RTCIceCandidate candy = (RTCIceCandidate) d;
            emmit(candy);
            Log.debug("local candidate " + d.toString());
        };
        /*
        RTCIceGatherOptions options;
        options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.NOHOST);
         */
        RTCIceGatherOptions options = googleStunOptions();
        gatherer.gather(options);
    }

    public RTCIceGatherOptions hostOptions() {
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.LOCAL);
        return options;
    }

    public RTCIceGatherOptions googleStunOptions() {
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.NOHOST);
        ArrayList<RTCIceServer> iceServers = new ArrayList< RTCIceServer>();
        ArrayList<URI> u = new ArrayList<URI>();
        try {
            u.add(new URI("stun:stun4.l.google.com:19302"));
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }

        String uname = null;
        String cred = null;
        RTCIceCredentialType credType = null;
        RTCIceServer e = new RTCIceServer(u, uname, cred, credType);
        iceServers.add(e);
        options.setIceServers(iceServers);
        return options;
    }

    static public void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        Log.setLevel(Log.INFO);
        Log.info("To test open chrome with the magic flag:");
        Log.info("--enable-blink-features=RTCIceTransportExtension");
        Log.info("Browse to https://steely-glint.github.io/slice/");
        new Slice();
    }

    void emmit(RTCIceCandidate candidate) {
        String ip = candidate.getIp();
        int port = candidate.getPort();
        Log.info("Copy this URL into the input box and press start.");
        Log.info("webrtc://" + this.lufrag + ":" + this.lupass + "@" + ip + ":" + port + "/");
    }
}
