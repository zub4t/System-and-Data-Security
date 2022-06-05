package p2p.communication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import p2p.Node;
import p2p.protocol.KademliaMessage;
import util.Util;

public class CommunicationInterface {
  DatagramChannel channel;

  public CommunicationInterface(DatagramChannel channel) {
    this.channel = channel;
  }

  public void send(byte[] b, InetSocketAddress receiver) {
    try {
      ByteBuffer bb = ByteBuffer.wrap(b);
      channel.send(bb, receiver);
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void send(Node node, KademliaMessage msg) {
    try {
      channel.send(ByteBuffer.wrap(Util.serializeMessage(msg)), node.getAddr());
      // System.out.println(" Sent " + bytesSent);
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void send(InetSocketAddress receiver, KademliaMessage msg) {
    try {
      int bytesSent = channel.send(
        ByteBuffer.wrap(Util.serializeMessage(msg)),
        receiver
      );
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void close() throws IOException {}
}
