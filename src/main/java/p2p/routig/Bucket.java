package p2p.routig;

import java.util.TreeSet;
import lombok.Data;
import p2p.Key;
import p2p.Node;
import p2p.Peer;

@Data
public class Bucket {
  private final int bucketId;

  final TreeSet<Node> nodes;
  private TreeSet<Node> kowns;
  private final int k;
  private final Peer p;

  public Bucket(Peer p, int k, int bucketId) {
    this.k = k;
    this.bucketId = bucketId;
    this.p = p;

    this.nodes = new TreeSet<>();
    this.kowns = new TreeSet<>();
  }

  public String calculate() {
    String hashValue = "";
    for (Node node : nodes) {
      hashValue += node.getId();
    }

    return hashValue;
  }

  public void addNode(Node node, String hash[]) {
    node.setLastSeen(System.currentTimeMillis());

    if (true) {
      kowns.add(node);
      hash[0] = calculate();
      if (nodes.size() < k) {
        if (!nodes.contains(node)) {
          node.setLastSeen(System.currentTimeMillis());
          nodes.add(node);
        } else {
          // System.out.println("This node is already in the bucket");
        }

        synchronized (RoutingTable.padlock) {
          hash[1] = calculate();
          RoutingTable.padlock.notify();
        }
      } else {
        Node last = nodes.last();
        if (last != null) {
          p.ping(last);
          new Thread(
            () -> {
              try {
                synchronized (RoutingTable.padlock) {
                  Thread.sleep(1000);
                  for (Key key : p.callers) {
                    System.out.println("key " + key);
                  }

                  if (!p.callers.contains(last.getId())) {
                    System.out.println("NÂO RESPONDEU ");
                    nodes.remove(last);
                    nodes.add(node);
                  } else {
                    System.out.println(" RESPONDEU ");

                    last.setLastSeen(System.currentTimeMillis());
                    p.callers.remove(last.getId());
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
    }
  }

  public TreeSet<Node> getNodes() {
    TreeSet<Node> set = new TreeSet<>();
    set.addAll(nodes);
    return set;
  }

  
}
