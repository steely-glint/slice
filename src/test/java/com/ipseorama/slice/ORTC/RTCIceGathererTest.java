/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceComponent;
import com.ipseorama.slice.ORTC.enums.RTCIceCredentialType;
import com.ipseorama.slice.ORTC.enums.RTCIceGatherPolicy;
import com.ipseorama.slice.ORTC.enums.RTCIceGathererState;
import com.phono.srtplight.Log;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class RTCIceGathererTest {

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
        List<RTCIceCandidate> lc = instance.getLocalCandidates();
        assert (lc.size() >0);
    }
    /*
    @Test
    public void testGatherReflex() throws URISyntaxException {
        
        //        "iceServers": [
        //    {urls: "stun:146.148.121.175:3478"},
        //    {urls: "turn:146.148.121.175:3478?transport=udp", 'credential': 'nexus5x', 'username': 'smartphone'},
        //    {url: "turn:146.148.121.175:443?transport=tcp", 'credential': 'nexus5x', 'username': 'smartphone'}
        //],
        
        System.out.println("gather");
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.ALL);
        ArrayList<RTCIceServer> iceServers = new ArrayList < RTCIceServer> ();
        ArrayList<URI> u = new ArrayList <URI>();
        URI stun = new URI("stun:146.148.121.175:3478");
        u.add(stun);
        String uname = null;
        String cred = null; 
        RTCIceCredentialType credType = null;
        RTCIceServer e = new RTCIceServer(u,uname,cred,credType);
        iceServers.add(e);
        options.setIceServers(iceServers);
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.gather(options);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
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
     * Test of setState method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testSetState() {
        System.out.println("setState");
        RTCIceGathererState _state = null;
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.setState(_state);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of getOnstatechange method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testGetOnstatechange() {
        System.out.println("getOnstatechange");
        RTCIceGatherer instance = new RTCIceGatherer();
        EventHandler expResult = null;
        EventHandler result = instance.getOnstatechange();
        assertEquals(expResult, result);
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
            synchronized (instance) {
                instance.notifyAll();
            }
        };
        RTCIceGatherOptions options = new RTCIceGatherOptions();
        options.setGatherPolicy(RTCIceGatherPolicy.ALL);
        ArrayList<RTCIceServer> iceServers = new ArrayList<RTCIceServer>();
        options.setIceServers(iceServers);
        instance.gather(options);
        synchronized (instance) {
            try {
                instance.wait(10000);
            } catch (InterruptedException ex) {
                ;
            }
        }
        RTCIceGathererState state = instance.getState();
        assert (state == RTCIceGathererState.GATHERING);
    }

    /**
     * Test of getOnerror method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testGetOnerror() {
        System.out.println("getOnerror");
        RTCIceGatherer instance = new RTCIceGatherer();
        EventHandler expResult = null;
        EventHandler result = instance.getOnerror();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of setOnerror method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testSetOnerror() {
        System.out.println("setOnerror");
        EventHandler onerror = null;
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.setOnerror(onerror);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of getOnlocalcandidate method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testGetOnlocalcandidate() {
        System.out.println("getOnlocalcandidate");
        RTCIceGatherer instance = new RTCIceGatherer();
        EventHandler expResult = null;
        EventHandler result = instance.getOnlocalcandidate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    /**
     * Test of setOnlocalcandidate method, of class RTCIceGatherer.
     */
    /*
    @Test
    public void testSetOnlocalcandidate() {
        System.out.println("setOnlocalcandidate");
        EventHandler onlocalcandidate = null;
        RTCIceGatherer instance = new RTCIceGatherer();
        instance.setOnlocalcandidate(onlocalcandidate);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
}
