/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

import com.ipseorama.slice.stun.StunTransactionManager;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 *
 * @author thp
 */
public interface IceEngine {

    public void start(DatagramSocket ds, StunTransactionManager tm);

    public boolean isStarted();

    public void addIceCreds(String user, String pass);

    public StunTransactionManager getTransactionManager();

    public void sendTo(byte[] buf, int off, int len, InetSocketAddress dtlsTo);

    public int getMTU();

}
