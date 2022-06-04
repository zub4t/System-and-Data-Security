package p2p.communication;

import p2p.Node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import p2p.protocol.KademliaMessage;
import util.Util;

public class CommunicationInterface {

    public CommunicationInterface() {

    }

    public void send(byte[] b, InetSocketAddress receiver) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(b);
            DatagramChannel channel = DatagramChannel.open();
            channel.send(bb, receiver);
            channel.close();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }

    public void send(Node node, KademliaMessage msg) {
        try {
            DatagramChannel channel = DatagramChannel.open();
            int bytesSent = channel.send(ByteBuffer.wrap(Util.serializeMessage(msg)), node.getAddr());

           // System.out.println(" Sent " + bytesSent);
            channel.close();
        } catch (Exception e) {

            System.out.println(e);
            e.printStackTrace();
        }

    }

    public void send(InetSocketAddress receiver, KademliaMessage msg) {
        try {
            DatagramChannel channel = DatagramChannel.open();
            int bytesSent = channel.send(ByteBuffer.wrap(Util.serializeMessage(msg)), receiver);

            //System.out.println(" Sent " + bytesSent);
            channel.close();
        } catch (Exception e) {

            System.out.println(e);
            e.printStackTrace();
        }

    }

    public void close() throws IOException {
    }
}