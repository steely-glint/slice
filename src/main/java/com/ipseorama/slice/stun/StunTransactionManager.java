/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.ORTC.RTCEventData;
import com.ipseorama.slice.ORTC.RTCIceCandidatePair;
import com.ipseorama.slice.ORTC.RTCIceTransport;
import com.ipseorama.slice.ORTC.RTCLocalIceCandidate;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.phono.srtplight.Log;
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
    private Queue<StunTransaction> transactions;
    Long nextDue = null;

    public StunTransactionManager() {
        transactions = new ConcurrentLinkedQueue();
    }

    public void addTransaction(StunTransaction t) {
        transactions.add(t);
        nextDue = null;
    }

    /*synchronized*/ StunTransaction get(byte[] tid) {
        StunTransaction t = transactions.stream().filter(
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
        Log.debug("recvd stun packet from " + p.getFar() + " id =" + StunPacket.hexString(p.getTid()));
        StunTransaction t = this.get(p.getTid());
        if (t != null) {
            t.receivedReply(p);
        } else {
            Log.verb("no matching transaction");
            if (getTransport() != null) {
                List<StunTransaction> trans = getTransport().receivedNew(p, prot, ipv);
                if (trans != null) {
                    for (StunTransaction tr : trans) {
                        transactions.add(tr);
                        Log.debug("added new transaction" + tr.toString());
                        if (p.hasAttribute("USE_CANDIDATE")) {
                            Log.debug("tid has USE_CANDIDATE set");
                        }
                    }
                } else {
                    Log.verb("didn't make transaction");
                }
            } else {
                Log.verb("no matching transport");
            }
        }
        nextDue = null; // situation has changed.
    }

    /*synchronized*/ public void removeComplete() {
        transactions.removeIf((StunTransaction t) -> {
            boolean ret = t.isComplete();
            if (ret) {
                Log.debug("removing " + t);
            }
            return ret;
        });
    }

    public void makeWork() { // RFC 8445 way
        if (transport != null) {
            RTCIceCandidatePair p = transport.nextCheck();
            if (p != null) {
                StunTransaction transact = p.queued(this.transport); // never nominate  on first pass...
                transact.onerror = (RTCEventData e) -> {
                    transport.onError(e);
                };
                Log.debug("Adding outbound transaction \n\t" + transact.toString() + "\n\t for " + p);
                addTransaction(transact);
            } else {
                Log.debug("Nothing to check");
            }
        }
    }

    /**
     *
     * @return the next time an action is due - or now + NAPTIME, whichever is
     * sooner
     */
    public long nextDue() {
        Iterator<StunTransaction> it = transactions.iterator();
        long ret = System.currentTimeMillis() + NAPLEN;
        while (it.hasNext()) {
            StunTransaction t = it.next();
            if (!t.isComplete() && (t.getDueTime() < ret)) {
                ret = t.getDueTime();
            }
        }
        nextDue = ret;
        return ret;
    }

    public List<StunPacket> transact(long now) {
        List<StunPacket> pkts = null;
        if ((nextDue == null) || (nextDue < now)) {
            pkts = transactions.stream()
                    .sorted((StunTransaction a, StunTransaction b) -> {
                        return (int) (a.dueTime - b.dueTime);
                    })
                    .filter((StunTransaction t) -> {
                        return (t != null) && !t.isComplete() && t.getDueTime() <= now;
                    }).map((StunTransaction t) -> {
                Log.debug("Building packet for " + t.toString() + "due at " + t.dueTime);
                return t.buildOutboundPacket();
            }).collect(Collectors.toList());
        } else {
            Log.verb("Nothing doing....");
        }
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
        return transactions.size();
    }

    public void pruneExcept(RTCIceCandidatePair sp, Long when) {
        Log.debug("Prune Transacts. to just " + sp);
        transactions.removeIf((StunTransaction sa) -> {
            boolean ret = true;
            if (sa instanceof IceStunBindingTransaction) {
                // things we want to remove
                // 1) other pair
                boolean others = (((IceStunBindingTransaction) sa).getPair()) != sp;
                // 2) anything that's completed 
                boolean complete = sa.isComplete();
                // 3) anything old
                boolean old = ((IceStunBindingTransaction) sa).dueTime < when;
                ret = (others || complete || old);
                Log.debug(ret ? "remove " : "keep " + sa.toString() + " (" + others + " " + complete + " " + old + ")");
            } else {
                Log.debug("remove " + sa + " not ICE trans");
            }
            return ret;
        });
    }

    public void listPairs() {
        if (transport != null){
            transport.listPairs();
        } else {
            Log.warn("No transport, so no pairs");
        }
    }

    public boolean pairHasActiveTrans(RTCIceCandidatePair sp) {
        return transactions.stream().anyMatch((StunTransaction sa) -> {
            Log.debug("----> active check " + sa);
            return (sa instanceof IceStunBindingTransaction)
                    && ((((IceStunBindingTransaction) sa).getPair()) == sp)
                    && !sa.isComplete()
                    ;
        });

    }

    public boolean localIsBusy(RTCIceCandidatePair icp) {
        RTCLocalIceCandidate local = icp.getLocal();
        return transactions.stream().anyMatch((StunTransaction sa) -> {
            Log.debug("----> busy check " + sa);
            return (sa instanceof IceStunBindingTransaction)
                    && ((((IceStunBindingTransaction) sa).getPair()).getLocal() == local)
                    && !sa.isComplete();
        });
    }

}
