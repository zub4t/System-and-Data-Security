package p2p.routig;

import lombok.Data;
import p2p.Node;
import p2p.Peer;
import p2p.communication.CommunicationInterface;
import p2p.protocol.KademliaMessage;
import util.Constants;
import util.Util;

import java.util.TreeSet;

/**
 * Created by Christoph on 22.09.2016.
 */
@Data
public class Bucket {
    private final int bucketId;

    private final TreeSet<Node> nodes;
    private final int k;
    private final CommunicationInterface communicationInterface;

    public Bucket(CommunicationInterface client, int k, int bucketId) {
        this.k = k;
        this.bucketId = bucketId;
        this.communicationInterface = client;

        this.nodes = new TreeSet<>();
    }

    public void addNode(Node node) {
        if (nodes.size() < k) {
            if (!nodes.contains(node)) {
                node.setLastSeen(System.currentTimeMillis());
                nodes.add(node);
            } else {
                System.out.println("This node is already in the bucket");
            }
        } else {
            Node last = nodes.last();
            try {
                KademliaMessage msg = KademliaMessage.builder().type(Constants.PING)
                        .localNode(Util.serializeNode(last))
                        .build();
                communicationInterface.send(last, msg);
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        if (Peer.callers.get(node.getId()) != null) {
                            nodes.remove(last);
                            node.setLastSeen(System.currentTimeMillis());
                            nodes.add(node);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();


            } catch (Exception e) {
                nodes.remove(last);
                nodes.add(node);
                return;
            }
        }
    }

    public TreeSet<Node> getNodes() {
        TreeSet<Node> set = new TreeSet<>();
        set.addAll(nodes);
        return set;
    }

    public void refreshBucket() {
        @SuppressWarnings("unchecked") TreeSet<Node> copySet = new TreeSet(nodes);
        // Check nodes on reachability and update
        copySet.stream().forEach(node -> {
            try {
                KademliaMessage msg = KademliaMessage.builder().type(Constants.PING)
                        .localNode(Util.serializeNode(node))
                        .build();
                communicationInterface.send(node, msg);
                //todo handle case where the node pinged can't be reached

                nodes.remove(node);
                node.setLastSeen(System.currentTimeMillis());
                nodes.add(node);

            } catch (Exception exp) {
                nodes.remove(node);
            }
        });
    }

}