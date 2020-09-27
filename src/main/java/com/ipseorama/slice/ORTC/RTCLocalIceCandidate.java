package com.ipseorama.slice.ORTC;

import com.ipseorama.slice.ORTC.enums.RTCIceCandidateType;
import com.ipseorama.slice.ORTC.enums.RTCIceProtocol;
import com.ipseorama.slice.ORTC.enums.RTCIceTcpCandidateType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 *
 * @author thp
 */
public class RTCLocalIceCandidate extends RTCIceCandidate {

    static RTCLocalIceCandidate mkTempCandidate(InetSocketAddress isoc, RTCIceProtocol prot, int ipversion, long pri, DatagramChannel ch) {
        InetAddress home = isoc.getAddress();
        String found = RTCIceCandidate.calcFoundation(RTCIceCandidateType.PRFLX, home, null, prot);
        RTCLocalIceCandidate cand = new RTCLocalIceCandidate(found,
                pri,
                home.getHostAddress(),
                RTCIceProtocol.UDP,
                (char) isoc.getPort(),
                RTCIceCandidateType.HOST,
                null, ch);
        cand.setIpVersion(ipversion);
        return cand;
    }

    private DatagramChannel channel;

    public RTCLocalIceCandidate(String foundation,
            long priority,
            String ip,
            RTCIceProtocol protocol,
            char port,
            RTCIceCandidateType type,
            RTCIceTcpCandidateType tcpType,
            DatagramChannel chan
    ) {
        super(foundation,
                priority,
                ip,
                protocol,
                port,
                type,
                tcpType);
        setChannel(chan);
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public void setChannel(DatagramChannel chan) {
        channel = chan;
    }

    /*@Override
    boolean sameEnough(RTCIceCandidate cand) {
        boolean ret = false;
        if (cand instanceof RTCLocalIceCandidate) {
            ret = (((RTCLocalIceCandidate) cand).getChannel() == this.channel);
            Log.debug("Using same channel - so treat as same local candidate");
        } else {
            ret = super.sameEnough(cand);
        }
        return ret;
    }*/
    
}
