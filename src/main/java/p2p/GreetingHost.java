package p2p;

import blockchain.InBlock;
import blockchain.OutBlock;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Timer;

import p2p.communication.CommunicationInterface;
import p2p.routig.RoutingTable;
import util.Util;

public class GreetingHost {

  public static void main(String[] args)
    throws InterruptedException, UnsupportedEncodingException {
    Node localNode = new Node(
      Key.random(),
      new InetSocketAddress("localhost", 2000),
      0
    );
    System.out.println("KEY:  " + localNode.getId());
    Peer p = new Peer(localNode);
    OutBlock outBlock = OutBlock
      .builder()
      .confimerdKey(true)
      .key(Key.random())
      .build();
    outBlock.setHash(outBlock.getKey().toString());
    outBlock.setInBlock(
      InBlock
        .builder()
        .previousHash(Key.random().toString())
        .content(Util.convertObjectToBytes("isso é uma menssagem inicial"))
        .contentType("init")
        .timeStamp(System.currentTimeMillis())
        .build()
    );
    p.previousBlocks.add(outBlock);
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
    Timer timer = new Timer();
    timer.schedule(new Task(p), 1000, 5000);
    service.start();

    service.join();
  }
}
