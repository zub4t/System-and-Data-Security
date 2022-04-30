package p2p.routig;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import p2p.Key;
import p2p.Node;
import p2p.Peer;
import p2p.protocol.KademliaMessage;
import util.Constants;
import util.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import blockchain.Block;

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
            buckets[i] = new Bucket(peer.communicationInterface, k, i);
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
        if (!node.getId().equals(localNodeId)) {
            String hash[] = new String[2];
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    buckets[getBucketId(node.getId())].addNode(node, hash);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }).start();
            synchronized (RoutingTable.padlock) {
                System.out.println("AGUARDADO");
                try {
                    padlock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("ACABO A ESPERA");
                System.out.println("OLD " + hash[0]);
                System.out.println("NEW " + hash[1]);
                if (!hash[1].equals(hash[0])) {
                    System.out.println("FOI ADICIONADO");

                    KademliaMessage msg = KademliaMessage.builder().build();
                    long seqNumber = new Random().nextLong();
                    msg.setType(Constants.PING);
                    msg.setSeqNumber(Util.longToBytes(seqNumber));
                    msg.setContent("PING".getBytes(StandardCharsets.UTF_8));
                    msg.setLocalNode(Util.serializeNode(node));
                    for (Bucket bucket : getBuckets()) {
                        for (Node n : bucket.getNodes()) {
                            if (n.getId() != node.getId()) {
                                peer.communicationInterface.send(n, msg);
                            }
                        }
                    }

                    System.out.println("UM CARA ADICIONADO");
                    HashMap<Key, Block> storageC = new HashMap<Key, Block>();
                    for (Map.Entry<Key, Block> entry : peer.storage.entrySet()) {
                        storageC.put(entry.getKey(), entry.getValue());

                    }
                    peer.storage.clear();
                    for (Map.Entry<Key, Block> entry : storageC.entrySet()) {
                        peer.store(entry.getValue());
                        System.out.println("Recalculando...");
                    }

                }

            }

        } else {
            System.out.println("Routing table of node=" + node.getId() + " can't contain itself. (localNodeId="
                    + localNodeId + ")");
        }
    }

    public Bucket[] getBuckets() {
        return buckets;
    }

    public Stream<Bucket> getBucketStream() {
        return Arrays.stream(buckets);
    }

    public List<Node> findClosest(Key lookupId, int numberOfRequiredNodes) {
        return getBucketStream().flatMap(bucket -> bucket.getNodes().stream())
                .sorted((node1, node2) -> node1.getId().getKey().xor(lookupId.getKey()).abs()
                        .compareTo(node2.getId().getKey().xor(lookupId.getKey()).abs()))
                .limit(numberOfRequiredNodes).collect(Collectors.toList());
    }

}