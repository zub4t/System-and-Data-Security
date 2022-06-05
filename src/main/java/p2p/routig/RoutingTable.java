package p2p.routig;

import blockchain.Block;
import blockchain.OutBlock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import p2p.Key;
import p2p.Node;
import p2p.Peer;

@ToString
@EqualsAndHashCode
public class RoutingTable {
  public static Object padlock = new Object();
  private final Key localNodeId;
  private final Bucket[] buckets;
  public Peer peer;

  public RoutingTable(int k, Key localNodeId, Peer peer) {
    this.localNodeId = localNodeId;
    this.peer = peer;
    buckets = new Bucket[Key.ID_LENGTH];
    for (int i = 0; i < Key.ID_LENGTH; i++) {
      buckets[i] = new Bucket(peer, k, i);
    }
  }

  /**
   * Compute the bucket ID in which a given node should be placed; the bucketId is
   * computed based on how far the node is away from the Local Node.
   *
   * @param nid The Key for which we want to find which bucket it belong to
   * @return Integer The bucket ID in which the given node should be placed.
   */
  public final int getBucketId(Key nid) {
    int bId = this.localNodeId.getDistance(nid) - 1;

    /*
     * If we are trying to insert a node into it's own routing table, then the
     * bucket ID will be -1, so let's just keep it in bucket 0
     */
    return bId < 0 ? 0 : bId;
  }

  public void addNode(Node node) {
    if (
      !node.getId().equals(localNodeId) &&
      !buckets[getBucketId(node.getId())].getNodes().contains(node)
    ) {
      String hash[] = new String[2];
      new Thread(
        () -> {
          try {
            Thread.sleep(100);
            buckets[getBucketId(node.getId())].addNode(node, hash);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      )
      .start();
      synchronized (RoutingTable.padlock) {
        // peer.log("AGUARDADO");
        try {
          padlock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (hash[1] != null && hash[0] != null && !hash[1].equals(hash[0])) {
          peer.log("New node added");

          HashMap<Key, Block> storageC = new HashMap<Key, Block>();
          for (Map.Entry<Key, Block> entry : peer.storage.entrySet()) {
            storageC.put(entry.getKey(), entry.getValue());
          }
          peer.storage.clear();
          for (Map.Entry<Key, Block> entry : storageC.entrySet()) {
            peer.store((OutBlock) entry.getValue());
            peer.log("Recalculating...");
          }
        }
      }
    }
  }

  public Bucket[] getBuckets() {
    return buckets;
  }

  public Stream<Bucket> getBucketStream() {
    return Arrays.stream(buckets);
  }
}
