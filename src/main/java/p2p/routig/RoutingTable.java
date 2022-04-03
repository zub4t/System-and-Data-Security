package p2p.routig;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import p2p.Key;
import p2p.Node;
import p2p.communication.CommunicationInterface;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@ToString
@EqualsAndHashCode
public class RoutingTable {

    private final Key localNodeId;

    private final Bucket[] buckets;
    public  CommunicationInterface communicationInterface;

    public RoutingTable(int k, Key localNodeId, CommunicationInterface communicationInterface) {
        this.localNodeId = localNodeId;
        this.communicationInterface = communicationInterface;
        buckets = new Bucket[Key.ID_LENGTH];
        for (int i = 0; i < Key.ID_LENGTH; i++) {
            buckets[i] = new Bucket(communicationInterface, k, i);
        }
    }

    /**
     * Compute the bucket ID in which a given node should be placed; the bucketId is computed based on how far the node is away from the Local Node.
     *
     * @param nid The Key for which we want to find which bucket it belong to
     * @return Integer The bucket ID in which the given node should be placed.
     */
    public final int getBucketId(Key nid) {
        int bId = this.localNodeId.getDistance(nid) - 1;

        /* If we are trying to insert a node into it's own routing table, then the bucket ID will be -1, so let's just keep it in bucket 0 */
        return bId < 0 ? 0 : bId;
    }

    public void addNode(Node node) {
        if (!node.getId().equals(localNodeId)) {
            buckets[getBucketId(node.getId())].addNode(node);
        } else {
             System.out.println("Routing table of node="+node.getId()+" can't contain itself. (localNodeId="+localNodeId+")");
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