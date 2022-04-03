package p2p;

import blockchain.Block;
import p2p.communication.CommunicationInterface;
import p2p.protocol.KademliaMessage;
import p2p.routig.Bucket;
import p2p.routig.RoutingTable;
import util.Util;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static util.Constants.*;

public class Peer {

    public Node localNode;
    private DatagramChannel inChannel;
    private RoutingTable routingTable;
    private HashMap<Key, Block> storage = new HashMap<>();
    public static HashMap<Key, Node> callers = new HashMap<Key, Node>();
    public static SocketChannel socketChannel = null;
    public HashMap<Key, Block> tempStorage = new HashMap<>();

    Peer(Node localNode, RoutingTable routingTable) {
        this.routingTable = routingTable;
        this.localNode = localNode;
        try {
            inChannel = DatagramChannel.open();
            InetSocketAddress iAdd = localNode.getAddr();
            inChannel.socket().bind(iAdd);
            // System.out.println("Server Started: " + iAdd);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Node localNode = new Node(Key.random(), new InetSocketAddress("localhost", Util.getRandomNumber(2000, 65535)),
                0);
        CommunicationInterface communicationInterface = new CommunicationInterface();
        RoutingTable routingTable = new RoutingTable(20, localNode.getId(), communicationInterface);
        Peer p = new Peer(localNode, routingTable);
        Thread service = p.startService();
        new Thread(() -> {
            try {
                (new AdmServer(p)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        service.start();
        service.join();

    }

    public Thread startService() {
        return new Thread(() -> {
            try {
                postOffice();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

    }

    public void addWellKnowPeer(String key, String ip, int port) {
        routingTable.addNode(Node.builder().addr(new InetSocketAddress(ip, port)).id(Key.build(key))
                .lastSeen(System.currentTimeMillis()).build());
    }

    public String listRoutingTable() {
        String s = "";
        s = ("Listing...\n");
        for (Bucket b : routingTable.getBuckets()) {
            s += ("*****" + b.getBucketId() + "*****\n");
            for (Node n : b.getNodes()) {
                s += (n) + "\n";

            }
            s += ("*****" + b.getBucketId() + "*****") + "\n";

        }
        return s;
    }

    public String listTempBlocks() {
        String str = "Blocks->\n";
        for (Map.Entry<Key, Block> entry : this.tempStorage.entrySet()) {
            str += (entry.getValue() + "\n");
        }
        return str;
    }

    public String listStoredBlocks() {
        String str = "Blocks->\n";
        for (Map.Entry<Key, Block> entry : this.storage.entrySet()) {
            str += (entry.getValue() + "\n");
        }
        return str;
    }

    public void ping(String IP, int PORT) {
        long seqNumber = new Random().nextLong();
        KademliaMessage outMessage = KademliaMessage.builder().build();
        try {
            outMessage.setType(PING);
            outMessage.setSeqNumber(Util.longToBytes(seqNumber));
            outMessage.setContent("PING".getBytes(StandardCharsets.UTF_8));
            outMessage.setLocalNode(Util.serializeNode(this.localNode));
            routingTable.communicationInterface.send(Util.serializeMessage(outMessage),
                    new InetSocketAddress(IP, PORT));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void store(Block block) {
        long seqNumber = new Random().nextLong();
        KademliaMessage outMessage = KademliaMessage.builder().build();
        try {
            outMessage.setType(STORE);
            outMessage.setSeqNumber(Util.longToBytes(seqNumber));
            outMessage.setContent(Util.serializeBlock(block));
            outMessage.setLocalNode(Util.serializeNode(this.localNode));
            _storage(block.key, outMessage, block);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void findValue(Key key) {
        long seqNumber = new Random().nextLong();
        KademliaMessage outMessage = KademliaMessage.builder().build();
        try {
            outMessage.setType(FIND_VALUE);
            outMessage.setSeqNumber(Util.longToBytes(seqNumber));
            outMessage.setContent(Util.serializeKey(key));
            outMessage.setLocalNode(Util.serializeNode(this.localNode));
            _findNodeOrValue(key, outMessage, localNode.getAddr());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void findNode(Key key) {
        long seqNumber = new Random().nextLong();
        KademliaMessage outMessage = KademliaMessage.builder().build();
        try {
            outMessage.setType(FIND_NODE);
            outMessage.setSeqNumber(Util.longToBytes(seqNumber));
            outMessage.setContent(Util.serializeKey(key));
            outMessage.setLocalNode(Util.serializeNode(this.localNode));
            _findNodeOrValue(key, outMessage, localNode.getAddr());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void _storage(Key key, KademliaMessage message, Block block) {
        int bucketId = routingTable.getBucketId(key);
        // System.out.println("STORE " + bucketId);
        if (bucketId == 0) {
            storage.put(block.getKey(), block);
        } else {
            if (routingTable.getBuckets()[bucketId].getNodes().size() > 0)
                for (Node node : routingTable.getBuckets()[bucketId].getNodes()) {
                    routingTable.communicationInterface.send(Util.serializeMessage(message), node.getAddr());
                }
            else {
                int closest = Integer.MAX_VALUE;
                int closestDistance = Integer.MAX_VALUE;

                for (Bucket bucket : routingTable.getBuckets()) {
                    if (bucket.getNodes().size() > 0) {
                        for (Node n : bucket.getNodes()) {
                            if (closestDistance > n.getId().getDistance(block.getKey())) {
                                closest = bucket.getBucketId();
                                closestDistance = n.getId().getDistance(block.getKey());
                            }
                        }

                    }
                }
                System.out.println("ID" + bucketId);
                if (closest != Integer.MAX_VALUE &&
                        closestDistance < localNode.getId().getDistance(block.getKey())) {
                    for (Node node : routingTable.getBuckets()[closest].getNodes()) {
                        routingTable.communicationInterface.send(Util.serializeMessage(message), node.getAddr());
                    }
                } else {
                    storage.put(block.getKey(), block);
                }
            }
        }
    }

    public void _findNodeOrValue(Key key, KademliaMessage message, InetSocketAddress receiver) {
        int bucketId = routingTable.getBucketId(key);
        // System.out.println("FINDING " + bucketId);
        if (bucketId == 0) {
            if (message.getType() == FIND_VALUE) {
                message.setContent(Util.serializeBlock(storage.get(key)));
                message.setType(FIND_VALUE_REPLY);
            } else {
                message.setContent(Util.serializeNode(localNode));
                message.setType(FIND_NODE_REPLY);
            }
            message.setLocalNode(Util.serializeNode(this.localNode));
            routingTable.communicationInterface.send(Util.serializeMessage(message), receiver);
        } else {
            if (routingTable.getBuckets()[bucketId].getNodes().size() > 0)
                for (Node node : routingTable.getBuckets()[bucketId].getNodes()) {
                    routingTable.communicationInterface.send(Util.serializeMessage(message), node.getAddr());
                }
            else {
                int closest = Integer.MAX_VALUE;
                int closestDistance = Integer.MAX_VALUE;

                for (Bucket bucket : routingTable.getBuckets()) {
                    if (bucket.getNodes().size() > 0) {
                        for (Node n : bucket.getNodes()) {
                            if (closestDistance > n.getId().getDistance(key)) {
                                closest = bucket.getBucketId();
                                closestDistance = n.getId().getDistance(key);
                            }
                        }

                    }
                }

                System.out.println("ID" + bucketId);
                System.out.println("closestDistance" + closestDistance);
                System.out.println(" storage.get(key) " + storage.get(key));
                if(closest > localNode.getId().getDistance(key)){
                    //sou eu quem deveria guardar :c
                    System.out.println("DEVERIA GUARDAR EU ");
                }


                if ((storage.get(key) == null) ) {
                    for (Node node : routingTable.getBuckets()[closest].getNodes()) {
                        routingTable.communicationInterface.send(Util.serializeMessage(message), node.getAddr());
                    }
                } else {
                  
                    if (message.getType() == FIND_VALUE) {
                        message.setContent(Util.serializeBlock(storage.get(key)));
                        message.setType(FIND_VALUE_REPLY);
                    } else {
                        message.setContent(Util.serializeNode(localNode));
                        message.setType(FIND_NODE_REPLY);
                    }
                    message.setLocalNode(Util.serializeNode(this.localNode));
                    routingTable.communicationInterface.send(Util.serializeMessage(message), receiver);
                }
            }
        }
    }

    private void postOffice() throws IOException {
        while (true) {

            try {

                ByteBuffer full = ByteBuffer.allocate(65507);
                inChannel.receive(full);
                byte[] m = new byte[full.position()];
                int bucketId = -1;
                full.flip();

                Block block;
                Node n;
                Node rn;
                Key key;
                KademliaMessage res;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                full.get(m);
                KademliaMessage message = Util.deserializeMessage(m);

                System.out.println(message);
                InetSocketAddress receiver = (Util.deserializeNode(message.getLocalNode())).getAddr();
                routingTable.addNode(Util.deserializeNode(message.getLocalNode()));
                /*
                 * if (message.getType() != STORE)
                 * for (Map.Entry<Key, Block> entry : this.storage.entrySet()) {
                 * int buketId = routingTable.getBucketId(entry.getKey());
                 * if (routingTable.getBuckets()[buketId].getNodes().size() > 0) {
                 * for (Node node : routingTable.getBuckets()[bucketId].getNodes()) {
                 * routingTable.communicationInterface.send(Util.serializeMessage(message),
                 * node.getAddr());
                 * }
                 * }
                 * }
                 */
                switch (message.getType()) {
                    case PING:
                        message.setType(PING_REPLY);
                        message.setLocalNode(Util.serializeNode(this.localNode));
                        routingTable.communicationInterface.send(Util.serializeMessage(message), receiver);
                        break;
                    case FIND_NODE:
                    case FIND_VALUE:
                        key = Util.deserializeKey(message.getContent());
                        _findNodeOrValue(key, message, receiver);
                        break;
                    case STORE:
                        block = Util.deserializeBlock(message.getContent());
                        _storage(block.key, message, block);
                        break;
                    case FIND_VALUE_REPLY:
                        rn = Util.deserializeNode(message.getLocalNode());
                        block = Util.deserializeBlock(message.getContent());
                        if (socketChannel != null) {
                            // socketChannel.write(ByteBuffer.wrap(("Searching result \n" + rn + "\n" +
                            // block.toString()).getBytes(StandardCharsets.UTF_8)));
                            System.out.println("***************************");
                            System.out.println(block);
                            System.out.println(rn);
                            System.out.println("***************************");

                            socketChannel
                                    .write(ByteBuffer.wrap(("Found ").getBytes(StandardCharsets.UTF_8)));

                        }
                        break;
                    case FIND_NODE_REPLY:
                        rn = Util.deserializeNode(message.getLocalNode());
                        if (socketChannel != null) {
                            socketChannel.write(ByteBuffer
                                    .wrap(("Searching result \n" + rn.toString()).getBytes(StandardCharsets.UTF_8)));
                        }
                        break;
                    case PING_REPLY:
                        callers.put(Util.deserializeNode(message.getLocalNode()).getId(),
                                Util.deserializeNode(message.getLocalNode()));
                        message.setType(STOP);
                        message.setLocalNode(Util.serializeNode(this.localNode));
                        routingTable.communicationInterface.send(Util.serializeMessage(message), receiver);
                        break;
                    case STOP:
                        // System.out.println("STOP");
                        break;

                    default:
                        throw new IllegalStateException("Unexpected value: " + message.getType());
                }
            } catch (Exception e) {
                e.printStackTrace();

            }

        }

    }

}
