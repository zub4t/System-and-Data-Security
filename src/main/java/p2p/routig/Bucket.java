package p2p.routig;

import java.util.TreeSet;
import lombok.Data;
import p2p.Node;
import p2p.Peer;
import p2p.communication.CommunicationInterface;
import p2p.protocol.KademliaMessage;
import util.Constants;
import util.Util;

@Data
public class Bucket {
  private final int bucketId;

  final TreeSet<Node> nodes;
  private final int k;
  private final CommunicationInterface communicationInterface;

  public Bucket(CommunicationInterface client, int k, int bucketId) {
    this.k = k;
    this.bucketId = bucketId;
    this.communicationInterface = client;

    this.nodes = new TreeSet<>();
  }

  public String calculate() {
    String hashValue = "";
    for (Node node : nodes) {
      hashValue += node.getId();
    }

    return hashValue;
  }

  public void addNode(Node node, String hash[]) {
    hash[0] = calculate();
    if (nodes.size() < k) {
      if (!nodes.contains(node)) {
        node.setLastSeen(System.currentTimeMillis());
        nodes.add(node);
      } else {
        System.out.println("This node is already in the bucket");
      }

      synchronized (RoutingTable.padlock) {
        hash[1] = calculate();
        RoutingTable.padlock.notify();
      }
    } else {
      Node last = nodes.last();
      KademliaMessage msg = KademliaMessage
        .builder()
        .type(Constants.PING)
        .localNode((last))
        .build();
      communicationInterface.send(last, msg);
      new Thread(
        () -> {
          try {
            synchronized (RoutingTable.padlock) {
              Thread.sleep(500);
              if (Peer.callers.get(last.getId()) == null) {
                nodes.remove(last);
                nodes.add(node);
              }
              hash[1] = calculate();
              RoutingTable.padlock.notify();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      )
      .start();
    }
  }

  public TreeSet<Node> getNodes() {
    TreeSet<Node> set = new TreeSet<>();
    set.addAll(nodes);
    return set;
  }

  public void refreshBucket() {
    @SuppressWarnings("unchecked")
    TreeSet<Node> copySet = new TreeSet(nodes);
    // Check nodes on reachability and update
    copySet
      .stream()
      .forEach(
        node -> {
          try {
            KademliaMessage msg = KademliaMessage
              .builder()
              .type(Constants.PING)
              .localNode((node))
              .build();
            communicationInterface.send(node, msg);
            // todo handle case where the node pinged can't be reached

            nodes.remove(node);
            node.setLastSeen(System.currentTimeMillis());
            nodes.add(node);
          } catch (Exception exp) {
            nodes.remove(node);
          }
        }
      );
  }
}
