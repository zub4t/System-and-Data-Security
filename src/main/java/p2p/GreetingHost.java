package p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import p2p.communication.CommunicationInterface;
import p2p.routig.RoutingTable;

public class GreetingHost {

  public static void main(String[] args) throws InterruptedException {
    Node localNode = new Node(
      Key.random(),
      new InetSocketAddress("localhost", 2000),
      0
    );
    System.out.println("KEY:  " + localNode.getId());
    Peer p = new Peer(localNode);
    p.communicationInterface = new CommunicationInterface();
    p.routingTable = new RoutingTable(20, localNode.getId(), p);
    Thread service = p.startService();

    new Thread(
      () -> {
        try {
          (new AdmServer(p)).start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    )
    .start();

    service.start();

    service.join();
  }
}
