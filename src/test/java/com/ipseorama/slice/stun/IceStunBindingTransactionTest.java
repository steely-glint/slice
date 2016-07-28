/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.phono.srtplight.Log;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.ShortBufferException;
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
public class IceStunBindingTransactionTest {

    public IceStunBindingTransactionTest() {
    }
    Map<String, String> passwords
            = Collections.unmodifiableMap(Stream.of(
                    new AbstractMap.SimpleEntry<>("o3rvX/IW811zoMmQ", "N3Wk6Gk45aUN3w9z3zql1ZqI"),
                    new AbstractMap.SimpleEntry<>("6hg", "4vc3fb0dshp2lhs6ekne716q0v"),
                    new AbstractMap.SimpleEntry<>("86u301ajf8mdpd", "1ir94ur2424gp08ta9eud4vtj1"),
                    new AbstractMap.SimpleEntry<>("LCIKuKTYRRaEp9hM", "ZuGwoWLM3IzMKlC6O6TJI9cg"),
                    new AbstractMap.SimpleEntry<>("pet", "snoopy"),
                    new AbstractMap.SimpleEntry<>("owner", "charliebrown"),
                    new AbstractMap.SimpleEntry<>("device", "bone"),
                    new AbstractMap.SimpleEntry<>("smartphone", "nexus5x"),
                    new AbstractMap.SimpleEntry<>("a88ag", "incorrect")
            ).collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

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

    /**
     * Test of buildOutboundPacket method, of class IceStunBindingTransaction.
     */
    @Test
    public void testBuildOutboundPacket() {
        try {
            System.out.println("buildOutboundPacket");
            String host = "8.8.8.8";
            int port = 12356;
            InetSocketAddress far = new InetSocketAddress(host, (char) port);
            InetSocketAddress near = new InetSocketAddress("4.4.4.4", (char) 3323);

            int reflexPri = 789;
            RTCIceRole role = RTCIceRole.CONTROLLING;
            long tiebreaker = 123456789;
            String outboundUser = "6hg:6hg";
            IceStunBindingTransaction instance = new IceStunBindingTransaction(host, port,
                    reflexPri,
                    role,
                    tiebreaker,
                    outboundUser);
            StunPacket send = instance.buildOutboundPacket();
            byte[] wire = send.outboundBytes("4vc3fb0dshp2lhs6ekne716q0v".getBytes());
            StunPacket rcv = StunPacket.mkStunPacket(wire, passwords,near);
            rcv.setFar(far);
            assert (rcv instanceof StunBindingRequest);
        } catch (Exception x) {
            if (Log.getLevel() >= Log.DEBUG) {
                x.printStackTrace();
            }
            fail("Exception thrown " + x.getMessage());

        }
    }

}
