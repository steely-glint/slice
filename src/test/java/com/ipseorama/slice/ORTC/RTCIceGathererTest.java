/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceCredentialType;
import com.ipseorama.slice.ORTC.enums.RTCIceGatherPolicy;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.ipseorama.slice.SingleThreadNioIceEngine;
import com.ipseorama.slice.ThreadedIceEngine;
import com.ipseorama.slice.stun.StunTransactionManager;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author thp
 */
public class RTCIceGathererTest {
    class DummyIceEngine implements IceEngine {

    public DummyIceEngine() {
    }

        @Override
        public void start(Selector ds, StunTransactionManager tm) {
        }

        @Override
        public void start(DatagramSocket ds, StunTransactionManager tm) {
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public void addIceCreds(String user, String pass) {
        }

        @Override
        public StunTransactionManager getTransactionManager() {
            return null;
        }

        @Override
        public void sendTo(byte[] buf, int off, int len, InetSocketAddress dtlsTo) throws IOException {
        }

        @Override
        public int getMTU() {
            return 1500;
        }

        @Override
        public long nextAvailableTime() {
            return System.currentTimeMillis();
        }
    
}
    public RTCIceGathererTest() {
        Log.setLevel(Log.ALL);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of close method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testClose() {
        System.out.println("close");
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.close();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of gather method, of class RTCIceGatherer.
     */
    @Test
    public void testGatherLocal() {
        System.out.println("------>gather");
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.ALL);
        ArrayList<RTCIceServer> iceServers = new ArrayList< RTCIceServer>();
        options.setIceServers(iceServers);
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.onlocalcandidate = (RTCEventData d) -> {
            Log.debug("local candidate " + d.toString());
            synchronized (instance) {
                instance.notifyAll();
            }
        };
        instance.gather(options);
        synchronized (instance) {
            try {
                instance.wait(10000);
            } catch (InterruptedException ex) {
                ;
            }
        }
        List<RTCLocalIceCandidate> lc = instance.getLocalCandidates();
        lc.forEach((_item) -> {
            Log.info(_item.toSDP(RTCIceComponent.RTP));
        });
        assert (lc.size() > 0);
    }

    //@Test
    public void testGatherReflex() throws URISyntaxException {

        //        "iceServers": [
        //    {urls: "stun:146.148.121.175:3478"},
        //    {urls: "turn:146.148.121.175:3478?transport=udp", 'credential': 'nexus5x', 'username': 'smartphone'},
        //    {url: "turn:146.148.121.175:443?transport=tcp", 'credential': 'nexus5x', 'username': 'smartphone'}
        //],
        System.out.println("gatherReflex");
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.NOHOST);
        ArrayList<RTCIceServer> iceServers = new ArrayList< RTCIceServer>();
        ArrayList<URI> u = new ArrayList<URI>();
        u.add(new URI("stun:146.148.121.175:3478"));
        u.add(new URI("stun:stun.noc.ams-ix.net:3478"));
        u.add(new URI("stun:stun.sipgate.net:3478"));

        String uname = null;
        String cred = null;
        RTCIceCredentialType credType = null;
        RTCIceServer e = new RTCIceServer(u, uname, cred, credType);
        iceServers.add(e);
        options.setIceServers(iceServers);
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.onlocalcandidate = (RTCEventData d) -> {
            Log.debug("local candidate " + d.toString());
            if (d instanceof RTCIceCandidate) {
                RTCIceCandidate can = (RTCIceCandidate) d;
                if (can.getType().equals(RTCIceCandidateType.SRFLX)) {
                    synchronized (instance) {
                        instance.notifyAll();
                    }
                }
            }
        };
        IceEngine tie = new SingleThreadNioIceEngine();
        instance.setIceEngine(tie);
        instance.gather(options);
        // TODO review the generated test code and remove the default call to fail.
        synchronized (instance) {
            try {
                instance.wait(30000);
            } catch (InterruptedException ex) {
                ;
            }
        }
        List<RTCLocalIceCandidate> cands = instance.getLocalCandidates();
        boolean m = cands.stream().anyMatch((RTCIceCandidate c) -> {
            return c.getType() == RTCIceCandidateType.SRFLX;
        });
        assert (m);
    }

    /**
     * Test of getLocalParameters method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testGetLocalParameters() {
        System.out.println("getLocalParameters");
        RTCIceGatherer instance = new RTCIceGatherer();
        RTCIceParameters expResult = null;
        RTCIceParameters result = instance.getLocalParameters();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of getComponent method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testGetComponent() {
        System.out.println("getComponent");
        RTCIceGatherer instance = new RTCIceGatherer();
        RTCIceComponent expResult = null;
        RTCIceComponent result = instance.getComponent();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of setComponent method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testSetComponent() {
        System.out.println("setComponent");
        RTCIceComponent _component = null;
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.setComponent(_component);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of setOnstatechange method, of class RTCIceGatherer.
     */
    @Test

    public void testSetOnstatechange() {
        System.out.println("----> setOnstatechange");
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.onstatechange = (RTCEventData d) -> {
            Log.debug("state is now " + d.toString());
            if (instance.getState() == RTCIceGathererState.COMPLETE) {
                synchronized (instance) {
                    instance.notifyAll();
                }
            }
        };
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.ALL);
        ArrayList<RTCIceServer> iceServers = new ArrayList<RTCIceServer>();
        options.setIceServers(iceServers);
        IceEngine tie = new SingleThreadNioIceEngine();
        instance.setIceEngine(tie);
        instance.gather(options);
        synchronized (instance) {
            try {
                instance.wait(30000);
            } catch (InterruptedException ex) {
                ;
            }
        }
        RTCIceGathererState state = instance.getState();
        instance.getLocalCandidates().forEach((RTCIceCandidate c) -> {
            Log.debug("local candidate " + c.toSDP(RTCIceComponent.RTP));
        });
        assert (state == RTCIceGathererState.COMPLETE);
    }

}
