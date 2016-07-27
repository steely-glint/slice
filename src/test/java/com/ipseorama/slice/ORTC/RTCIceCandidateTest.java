/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.phono.srtplight.Log;
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
public class RTCIceCandidateTest {

    public RTCIceCandidateTest() {
        Log.setLevel(Log.DEBUG);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    RTCIceCandidate instance = null;

    @Before
    public void setUp() {
        String foundation = "3";
        int component = 1;
        RTCIceProtocol prot = RTCIceProtocol.UDP;
        long priority = 16777215L;
        String ip = "146.148.121.175";
        int port = 54221;
        RTCIceCandidateType type = RTCIceCandidateType.RELAY;
        instance = new RTCIceCandidate(foundation,
                priority,
                ip,
                prot,
                (char) port,
                type,
                null);
        instance.setRelatedAddress("192.67.4.18");
        instance.setRelatedPort((char) 5125);
        instance.setIpVersion(4);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getFoundation method, of class RTCIceCandidate.
     */
    @Test
    public void testGetFoundation() {
        System.out.println("getFoundation");
        String expResult = "3";
        String result = instance.getFoundation();
        assertEquals(expResult, result);
    }

    /**
     * Test of setFoundation method, of class RTCIceCandidate.
     */
    @Test
    public void testSetFoundation() {
        System.out.println("setFoundation");
        String foundation = "6";
        instance.setFoundation(foundation);
        String result = instance.getFoundation();
        assertEquals(foundation, result);
    }

    /**
     * Test of getPriority method, of class RTCIceCandidate.
     */
    @Test
    public void testGetPriority() {
        System.out.println("getPriority");
        long expResult = 16777215L;
        long result = instance.getPriority();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPriority method, of class RTCIceCandidate.
     */
    @Test
    public void testSetPriority() {
        System.out.println("setPriority");
        long priority = 1000L;
        instance.setPriority(priority);
        long result = instance.getPriority();
        assertEquals(priority, result);
    }

    /**
     * Test of getIp method, of class RTCIceCandidate.
     */
    @Test
    public void testGetIp() {
        System.out.println("getIp");
        String expResult = "146.148.121.175";
        String result = instance.getIp();
        assertEquals(expResult, result);
    }

    /**
     * Test of setIp method, of class RTCIceCandidate.
     */
    @Test
    public void testSetIp() {
        System.out.println("setIp");
        String ip = "8.8.8.8";
        instance.setIp(ip);
        String result = instance.getIp();
        assertEquals(ip, result);
    }

    /**
     * Test of getProtocol method, of class RTCIceCandidate.
     */
    @Test
    public void testGetProtocol() {
        System.out.println("getProtocol");
        RTCIceProtocol expResult = RTCIceProtocol.UDP;
        RTCIceProtocol result = instance.getProtocol();
        assertEquals(expResult, result);
    }

    /**
     * Test of setProtocol method, of class RTCIceCandidate.
     */
    @Test
    public void testSetProtocol() {
        System.out.println("setProtocol");
        RTCIceProtocol protocol = RTCIceProtocol.TCP;
        instance.setProtocol(protocol);
        RTCIceProtocol result = instance.getProtocol();
        assertEquals(protocol, result);
    }

    /**
     * Test of getPort method, of class RTCIceCandidate.
     */
    @Test
    public void testGetPort() {
        System.out.println("getPort");
        char expResult = (char) 54221;
        char result = instance.getPort();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPort method, of class RTCIceCandidate.
     */
    @Test
    public void testSetPort() {
        System.out.println("setPort");
        char port = (char) 1;
        instance.setPort(port);
        char result = instance.getPort();
        assertEquals(port, result);
    }

    /**
     * Test of getType method, of class RTCIceCandidate.
     */
    @Test
    public void testGetType() {
        System.out.println("getType");
        RTCIceCandidateType expResult = RTCIceCandidateType.RELAY;
        RTCIceCandidateType result = instance.getType();
        assertEquals(expResult, result);
    }

    /**
     * Test of setType method, of class RTCIceCandidate.
     */
    @Test
    public void testSetType() {
        System.out.println("setType");
        RTCIceCandidateType type = RTCIceCandidateType.SRFLX;
        instance.setType(type);
        RTCIceCandidateType result = instance.getType();
        assertEquals(type, result);
    }

    /**
     * Test of getRelatedAddress method, of class RTCIceCandidate.
     */
    @Test
    public void testGetRelatedAddress() {
        System.out.println("getRelatedAddress");
        String expResult = "192.67.4.18";
        String result = instance.getRelatedAddress();
        assertEquals(expResult, result);
    }

    /**
     * Test of setRelatedAddress method, of class RTCIceCandidate.
     */
    @Test
    public void testSetRelatedAddress() {
        System.out.println("setRelatedAddress");
        String relatedAddress = "4.4.4.4";
        instance.setRelatedAddress(relatedAddress);
        String result = instance.getRelatedAddress();
        assertEquals(relatedAddress, result);
    }

    /**
     * Test of getRelatedPort method, of class RTCIceCandidate.
     */
    @Test
    public void testGetRelatedPort() {
        System.out.println("getRelatedPort");
        char expResult = (char) 5125;
        char result = instance.getRelatedPort();
        assertEquals(expResult, result);
    }

    /**
     * Test of setRelatedPort method, of class RTCIceCandidate.
     */
    @Test
    public void testSetRelatedPort() {
        System.out.println("setRelatedPort");
        char relatedPort = 5555;
        instance.setRelatedPort(relatedPort);
        char result = instance.getRelatedPort();
        assertEquals(relatedPort, result);
    }

    /**
     * Test of toString method, of class RTCIceCandidate.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        String expResult = "candidate:3 1 udp 16777215 146.148.121.175 54221 typ relay generation 0 raddr 192.67.4.18 rport 5125";
        String result = instance.toString();
        Log.debug(result);
        Log.debug(expResult);

        assertEquals(expResult, result);
    }

}
