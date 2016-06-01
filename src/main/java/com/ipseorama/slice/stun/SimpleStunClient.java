/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.phono.srtplight.Log;
import java.net.InetSocketAddress;


/**
 *
 * @author tim
 */
public class SimpleStunClient extends StunPacketReceiver{


    public void setStunServer(InetSocketAddress stun) {


    }

    public void getReflexAddress(ReflexAddressCallBack rcb) {

    }

    interface AsyncCallBack {
        void timedOut();
    }

    interface ReflexAddressCallBack extends AsyncCallBack {
        void newReflexAddress(InetSocketAddress reflex);
    }

    public static void main(String argv[]) {
        InetSocketAddress stun = new InetSocketAddress("stun.l.google.com", 19302);
        SimpleStunClient mine = new SimpleStunClient();
        mine.setStunServer(stun);
        ReflexAddressCallBack rcb = new ReflexAddressCallBack() {
            @Override
            public void newReflexAddress(InetSocketAddress reflex) {
                Log.debug("got reflex address "+reflex.toString());
                this.notifyAll();
            }

            @Override
            public void timedOut() {
                Log.debug("Not got reflex address within timeout ");
                this.notifyAll();
            }

        };
        mine.getReflexAddress(rcb);
        try {
            synchronized (rcb) {
                rcb.wait(60000);
            }
        } catch (InterruptedException ex) {
            ;
        }
    }
}
