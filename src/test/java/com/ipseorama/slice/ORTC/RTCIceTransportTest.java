/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidatePairState;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceTransportState;
import com.phono.srtplight.Log;
import java.io.StringReader;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thp
 */
public class RTCIceTransportTest {

    private static RTCLocalIceCandidate mkCandy(JsonObject jo) {
        RTCLocalIceCandidate ret = new RTCLocalIceCandidate(jo.getString("foundation"),
                Long.parseLong(jo.getString("priority", "0")),
                jo.getString("ip"),
                RTCIceProtocol.fromString(jo.getString("protocol", "tcp")),
                (char) Integer.parseInt(jo.getString("port", "0")),
                RTCIceCandidateType.fromString(jo.getString("type", "host")),
                null,null);
        if (jo.getString("ip").contains(".")) {
            ret.setIpVersion(4);
        }
        if (jo.containsKey("rport") && jo.containsKey("raddr")) {
            ret.setRelatedAddress(jo.getString("raddr"));
            ret.setRelatedPort((char) Integer.parseInt(jo.getString("rport")));
        }
        return ret;
    }

    private static RTCLocalIceCandidate[] mkLocals() {
        return parseJsonCandy(localsJson);
    }

    private static RTCIceCandidate[] mkRemotes() {
        return parseJsonCandy(remotesJson);
    }

    private static RTCLocalIceCandidate[] parseJsonCandy(String json) {
        StringReader s = new StringReader(json);
        JsonReader reader = Json.createReader(s);
        JsonArray lcs = (JsonArray) reader.read();
        RTCLocalIceCandidate[] ret = new RTCLocalIceCandidate[lcs.size()];
        for (int o = 0; o < ret.length; o++) {
            JsonObject jo = lcs.getJsonObject(o);
            ret[o] = mkCandy(jo);
        }
        return ret;
    }

    public RTCIceTransportTest() {
    }
    static String localsJson = "["
            + "{\"foundation\":\"2169522962\",\"component\":\"1\",\"protocol\":\"tcp\",\"priority\":\"1518214911\",\"ip\":\"192.67.4.33\",\"port\":\"9\",\"type\":\"host\",\"generation\":\"0\"},"
            + "{\"foundation\":\"3423404818\",\"component\":\"1\",\"protocol\":\"tcp\",\"priority\":\"1518283007\",\"ip\":\"2a01:348:339::4d4e:d55b:3814:9bbb\",\"port\":\"9\",\"type\":\"host\",\"generation\":\"0\"},"
            + "{\"foundation\":\"3095023806\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"41820159\",\"ip\":\"146.148.121.175\",\"port\":\"57250\",\"type\":\"relay\",\"raddr\":\"192.67.4.33\",\"rport\":\"59240\",\"generation\":\"0\"},"
            + "{\"foundation\":\"3486758882\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"2122194687\",\"ip\":\"192.67.4.33\",\"port\":\"59240\",\"type\":\"host\",\"generation\":\"0\"},"
            + "{\"foundation\":\"2190409698\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"2122262783\",\"ip\":\"2a01:348:339::4d4e:d55b:3814:9bbb\",\"port\":\"58585\",\"type\":\"host\",\"generation\":\"0\"}"
            + "]";
    RTCLocalIceCandidate locals[];

    static String remotesJson = "["
            + "{\"foundation\":\"3\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"16777215\",\"ip\":\"146.148.121.175\",\"port\":\"58930\",\"type\":\"relay\",\"generation\":\"0\",\"raddr\":\"192.67.4.167\",\"rport\":\"11931\"},"
            + "{\"foundation\":\"1\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"2130706431\",\"ip\":\"2a01:348:339:0:e291:f5ff:fe50:171d\",\"port\":\"13019\",\"type\":\"host\",\"generation\":\"0\"},"
            + "{\"foundation\":\"2\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"2130706431\",\"ip\":\"fe80:0:0:0:e291:f5ff:fe50:171d\",\"port\":\"13019\",\"type\":\"host\",\"generation\":\"0\"},"
            + "{\"foundation\":\"3\",\"component\":\"1\",\"protocol\":\"udp\",\"priority\":\"2113932031\",\"ip\":\"192.67.4.167\",\"port\":\"13019\",\"type\":\"host\",\"generation\":\"0\"}"
            + "]";

    @BeforeClass
    public static void setUpClass() {
        Log.setLevel(Log.DEBUG);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        locals = mkLocals();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getRTCIceTransportState method, of class RTCIceTransport.
     */
    /*
    @Test
    public void testGetRTCIceTransportState() {
        System.out.println("getRTCIceTransportState");
        RTCIceTransport instance = null;
        RTCIceTransportState expResult = null;
        RTCIceTransportState result = instance.getRTCIceTransportState();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of getRemoteCandidates method, of class RTCIceTransport.
     */
    /*
    @Test
    public void testGetRemoteCandidates() {
        System.out.println("getRemoteCandidates");
        RTCIceTransport instance = null;
        List<RTCIceCandidate> expResult = null;
        List<RTCIceCandidate> result = instance.getRemoteCandidates();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of getSelectedCandidatePair method, of class RTCIceTransport.
     */
    /*
    @Test
    public void testGetSelectedCandidatePair() {
        System.out.println("getSelectedCandidatePair");
        RTCIceTransport instance = null;
        RTCIceCandidatePair expResult = null;
        RTCIceCandidatePair result = instance.getSelectedCandidatePair();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of start method, of class RTCIceTransport.
     */
    @Test
    public void testStart() {
        System.out.println("start");
        RTCIceGatherer gatherer = mkMockGatherer();
        RTCIceParameters remoteParameters = null;
        RTCIceRole role = RTCIceRole.CONTROLLING;
        RTCIceTransport instance = new RTCIceTransport(gatherer, role, RTCIceComponent.RTP);
        instance.start(gatherer, remoteParameters, role);
        assertEquals(0, instance.remoteCandidates.size());
    }

    /**
     * Test of stop method, of class RTCIceTransport.
     */
    /*
    @Test
    public void testStop() {
        System.out.println("stop");
        RTCIceTransport instance = null;
        instance.stop();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of getRemoteParameters method, of class RTCIceTransport.
     */
    /*
    @Test
    public void testGetRemoteParameters() {
        System.out.println("getRemoteParameters");
        RTCIceTransport instance = null;
        RTCIceParameters expResult = null;
        RTCIceParameters result = instance.getRemoteParameters();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of addRemoteCandidate method, of class RTCIceTransport.
     */
    @Test
    public void testAddRemoteCandidate() {
        System.out.println("addRemoteCandidate");
        RTCIceGatherer gatherer = mkMockGatherer();
        RTCIceParameters remoteParameters = null;
        RTCIceRole role = RTCIceRole.CONTROLLING;
        RTCIceTransport instance = new RTCIceTransport(gatherer, role, RTCIceComponent.RTP);
        instance.start(gatherer, remoteParameters, role);
        assertEquals(0, instance.remoteCandidates.size());
        RTCIceCandidate[] rems = mkRemotes();
        for (RTCIceCandidate r : rems) {
            instance.addRemoteCandidate(r);
        }
        assertEquals(rems.length, instance.remoteCandidates.size());
    }

    @Test
    public void testNextCheck() {
        System.out.println("NextCheck");
        RTCIceGatherer gatherer = mkMockGatherer();
        RTCIceParameters remoteParameters = null;
        RTCIceRole role = RTCIceRole.CONTROLLING;
        RTCIceTransport instance = new RTCIceTransport(gatherer, role, RTCIceComponent.RTP);
        instance.start(gatherer, remoteParameters, role);
        assertEquals(0, instance.remoteCandidates.size());
        RTCIceCandidate[] rems = mkRemotes();
        for (RTCIceCandidate r : rems) {
            instance.addRemoteCandidate(r);
        }
        assertFalse(instance.candidatePairs.isEmpty());
        RTCIceCandidatePair cp;
        int n = 0;
        while (null != (cp = instance.nextCheck())) {
            Log.debug("next candidate pair is :" + cp.toString());
            cp.setState(RTCIceCandidatePairState.INPROGRESS);
            n++;
        }
        assertEquals(6, n);
    }

    @Test
    public void testMaxPairs() {
        System.out.println("MaxPairs");
        int mcc = RTCIceTransport.MAXCHECKS;
        RTCIceTransport.MAXCHECKS = 3;
        RTCIceGatherer gatherer = mkMockGatherer();
        RTCIceParameters remoteParameters = null;
        RTCIceRole role = RTCIceRole.CONTROLLING;
        RTCIceTransport instance = new RTCIceTransport(gatherer, role, RTCIceComponent.RTP);
        instance.start(gatherer, remoteParameters, role);
        assertEquals(0, instance.remoteCandidates.size());
        RTCIceCandidate[] rems = mkRemotes();
        for (RTCIceCandidate r : rems) {
            instance.addRemoteCandidate(r);
        }
        assertFalse(instance.candidatePairs.isEmpty());
        RTCIceCandidatePair cp;
        int n = 0;
        while (null != (cp = instance.nextCheck())) {
            Log.debug("next candidate pair is :" + cp.toString());
            cp.setState(RTCIceCandidatePairState.INPROGRESS);
            n++;
        }
        RTCIceTransport.MAXCHECKS = mcc;
        assertEquals(3, n);
    }

    private RTCIceGatherer mkMockGatherer() {
        RTCIceGatherer mock = new RTCIceGatherer() {
            @Override
            public void gather(RTCIceGatherOptions options) {
                setState(RTCIceGathererState.GATHERING);
                for (RTCLocalIceCandidate l : locals) {
                    addLocalCandidate(l);
                }
            }
        };
        mock.gather(null);
        return mock;
    }
    /**
     * Test of setRemoteCandidates method, of class RTCIceTransport.
     */
    /*
    @Test
    public void testSetRemoteCandidates() {
        System.out.println("setRemoteCandidates");
        List<RTCIceCandidate> remoteCandidates = null;
        RTCIceTransport instance = null;
        instance.setRemoteCandidates(remoteCandidates);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
}
