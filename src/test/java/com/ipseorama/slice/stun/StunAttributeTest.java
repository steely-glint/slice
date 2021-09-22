/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
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
public class StunAttributeTest {

    public StunAttributeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        Log.setLevel(Log.DEBUG);
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

    @Test
    public void testString() {
        StunAttribute a = new StunAttribute("USERNAME");
        String s = "Test Name";
        a.setString(s);
        String s1 = a.getString();
        assertEquals("String should match", s, s1);
        assertEquals("Type should match", "USERNAME", a.getName());
        ByteBuffer out = ByteBuffer.allocate(1024);
        int len = a.put(out);
        ((Buffer)out).position(0);
        StunAttribute a1 = new StunAttribute(new Integer(out.getChar()), out.getChar(), out);
        assertEquals("Type should match", "USERNAME", a1.getName());
        String s2 = a1.getString();
        assertEquals("String should match", s, s2);
    }

    @Test
    public void testIP() {
        StunAttribute a = new StunAttribute("MAPPED-ADDRESS");
        InetSocketAddress s = new InetSocketAddress("192.67.4.1", 33333);
        a.setIpAddress(s);
        try {
            InetSocketAddress s1 = a.getIpAddress();
            assertEquals("String should match", s, s1);
            assertEquals("Type should match", "MAPPED-ADDRESS", a.getName());
            ByteBuffer out = ByteBuffer.allocate(1024);
            int len = a.put(out);
            ((Buffer)out).position(0);
            StunAttribute a1 = new StunAttribute(new Integer(out.getChar()), out.getChar(), out.slice());
            assertEquals("Type should match", "MAPPED-ADDRESS", a1.getName());
            InetSocketAddress s2 = a1.getIpAddress();
            assertEquals("String should match", s, s2);
        } catch (UnknownHostException ex) {
            fail("Unknown host");
        }
    }

    @Test
    public void testBytes() {
        StunAttribute a = new StunAttribute("RESERVATION-TOKEN");
        byte[] rtok = new byte[8];
        Random r = new Random(7); // insecure random fixed seed for repeatable builds
        r.nextBytes(rtok);
        a.setBytes(rtok);
        byte[] rtok1 = a.getBytes();
        org.junit.Assert.assertArrayEquals("bytes should match", rtok, rtok1);
        assertEquals("Type should match", "RESERVATION-TOKEN", a.getName());
        ByteBuffer out = ByteBuffer.allocate(1024);
        int len = a.put(out);
        ((Buffer)out).position(0);
        StunAttribute a1 = new StunAttribute(new Integer(out.getChar()), out.getChar(), out.slice());
        assertEquals("Type should match", "RESERVATION-TOKEN", a1.getName());
        byte[] rtok2 = a1.getBytes();
        org.junit.Assert.assertArrayEquals("bytes should match", rtok, rtok2);
    }

        @Test
    public void testShorts() {
        StunAttribute a = new StunAttribute("UNKNOWN-ATTRIBUTES");
        Object[] attrs =  StunAttribute.__typeMap.keySet().toArray();
        short[] ukna = new short[attrs.length];
        for (int i=0;i<ukna.length;i++){
            ukna[i] = ((Integer)attrs[i]).shortValue();
        }
        a.setShorts(ukna);
        short[] ukna1 = a.getShorts();
        org.junit.Assert.assertArrayEquals("shorts should match", ukna, ukna1);
        assertEquals("Type should match", "UNKNOWN-ATTRIBUTES", a.getName());
        ByteBuffer out = ByteBuffer.allocate(1024);
        int len = a.put(out);
        ((Buffer)out).position(0);
        StunAttribute a1 = new StunAttribute(new Integer(out.getChar()), out.getChar(), out.slice());
        assertEquals("Type should match", "UNKNOWN-ATTRIBUTES", a1.getName());
        short[] ukna2 = a1.getShorts();
        org.junit.Assert.assertArrayEquals("bytes should match", ukna, ukna2);
    }
}
