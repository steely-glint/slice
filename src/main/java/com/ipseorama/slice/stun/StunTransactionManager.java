/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCIceCandidatePair;
import com.ipseorama.slice.ORTC.RTCIceTransport;
import com.ipseorama.slice.ORTC.enums.RTCIceCandidatePairState;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.phono.srtplight.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 *
 * @author thp This class manages a list of transactions - it extends HashMap
 * and adds convenience methods
 */
public class StunTransactionManager {

    long NAPLEN = 1000;
    private RTCIceTransport transport;
    private Queue<StunTransaction> values;

    public StunTransactionManager() {
        values = new ConcurrentLinkedQueue();
    }

    public void addTransaction(StunTransaction t) {
        values.add(t);
    }

    /*synchronized*/ StunTransaction get(byte[] tid) {
        StunTransaction t = values.stream().filter(
                (StunTransaction tt) -> {
                    boolean found = Arrays.equals(tid, tt.id);
                    Log.verb("" + found + " matching " + StunPacket.hexString(tt.id));
                    return found;
                }).findAny().orElse(null);
        return t;
    }

    IceStunBindingTransaction getIceBindingTrans(byte[] tid) {
        Log.verb("incomming packet with tid " + StunPacket.hexString(tid));
        IceStunBindingTransaction ret = null;
        StunTransaction t = get(tid);
        if (t != null) {
            Log.verb("found matching " + t.getClass().getSimpleName() + " for tid " + StunPacket.hexString(tid));
        } else {
            Log.debug("no matching transaction for tid " + StunPacket.hexString(tid));
        }
        if (t instanceof IceStunBindingTransaction) {
            ret = (IceStunBindingTransaction) t;
        } else {
            Log.debug("not an IceStunBindingTransaction for tid " + StunPacket.hexString(tid));
        }
        return ret;
    }

    public void receivedPacket(StunPacket p, RTCIceProtocol prot, int ipv) {
        Log.debug("recvd stun packet from " + p.getFar());
        StunTransaction t = this.get(p.getTid());
        if (t != null) {
            t.received(p);
        } else {
            Log.verb("no matching transaction");
            if (getTransport() != null) {
                List<StunTransaction> trans = getTransport().received(p, prot, ipv);
                if (trans != null) {
                    for (StunTransaction tr : trans) {
                        values.add(tr);
                        Log.debug("added new transaction" + tr.toString());
                    }
                } else {
                    Log.verb("didn't make transaction");
                }
            } else {
                Log.verb("no matching transport");
            }
        }
    }

    /*synchronized*/ public void removeComplete() {
        values.removeIf((StunTransaction t) -> {
            boolean ret = t.isComplete();
            if (ret){
                Log.debug("removing " +t);
            }
            return ret;
        });
    }

    /**
     *
     * @return the next time an action is due - or now + NAPTIME, whichever is
     * sooner
     */
    public long nextDue() {
        Iterator<StunTransaction> it = values.iterator();
        long ret = System.currentTimeMillis() + NAPLEN;
        while (it.hasNext()) {
            StunTransaction t = it.next();
            if (!t.isComplete() && (t.getDueTime() < ret)) {
                ret = t.getDueTime();
            }
        }
        return ret;
    }

    /*synchronized*/ public List<StunPacket> transact(long now) {
        List<StunPacket> pkts = values.stream().filter((StunTransaction t) -> {
            return (t != null) && !t.isComplete() && t.getDueTime() <= now;
        }).map((StunTransaction t) -> {
            Log.debug("Building packet for " + t.toString());
            return t.buildOutboundPacket();
        }).collect(Collectors.toList());
        return pkts;
    }

    /**
     * @return the transport
     */
    public RTCIceTransport getTransport() {
        return transport;
    }

    /**
     * @param transport the transport to set
     */
    public void setTransport(RTCIceTransport transport) {
        this.transport = transport;
    }

    public int size() {
        return values.size();
    }

    public RTCIceCandidatePair findValidNominatedPair() {
        return this.transport == null ? null : transport.findValidNominatedPair();
    }

    public void maybeAddTransactionForPair(RTCIceCandidatePair p) {
        StunTransaction transact = p.queued(this.transport);
        final RTCIceCandidatePair mypair = p;
        mypair.setState(RTCIceCandidatePairState.INPROGRESS);
        transact.oncomplete = (RTCEventData e) -> {
            Log.verb("Queued trans check complete. do something here....");
            //to do: some state update on the pair here.
            mypair.updateState(e);
        };
        transact.onerror = (RTCEventData e) -> {
            transport.onError(e);
        };
        Log.debug("Adding outbound transaction for " + p);
        addTransaction(transact);
    }

}
