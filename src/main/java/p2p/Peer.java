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
    public RoutingTable routingTable;
    public HashMap<Key, Block> storage = new HashMap<>();
    public static HashMap<Key, Node> callers = new HashMap<Key, Node>();
    public static SocketChannel socketChannel = null;
    public HashMap<Key, Block> tempStorage = new HashMap<>();
    public CommunicationInterface communicationInterface;
    public Node lastSearchedNode;
    public Block lastSearchedBlock;

    Peer(Node localNode) {
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
        Peer p = new Peer(localNode);
        p.communicationInterface = new CommunicationInterface();
        p.routingTable = new RoutingTable(20, localNode.getId(), p);

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
            this.communicationInterface.send(Util.serializeMessage(outMessage),
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
        int closest = Integer.MAX_VALUE;
        int closestDistance = Integer.MAX_VALUE;
        Node nn = null;
        for (Bucket bucket : routingTable.getBuckets()) {
            if (bucket.getNodes().size() > 0) {
                if (Math.abs(bucket.getBucketId() - bucketId) < closestDistance) {
                    closestDistance = Math.abs(bucket.getBucketId() - bucketId);
                    closest = bucket.getBucketId();
                    nn = bucket.getNodes().first();
                }
            }
        }
        System.out.println("closest" + closest);
        System.out.println("minha " + routingTable.getBucketId(key) + " ");

        if (routingTable.getBucketId(key) < closest || closest == Integer.MAX_VALUE) {
            storage.put(block.getKey(), block);
        } else if (storage.get(key) == null && closest != Integer.MAX_VALUE) {
            for (Node node : routingTable.getBuckets()[closest].getNodes()) {
                this.communicationInterface.send(Util.serializeMessage(message), node.getAddr());
            }
            if (routingTable.getBucketId(nn.getId()) == closest) {
                storage.put(block.getKey(), block);

            }

        }

    }

    public void _findNodeOrValue(Key key, KademliaMessage message, InetSocketAddress receiver) {
        int bucketId = routingTable.getBucketId(key);
        int closest = Integer.MAX_VALUE;
        int closestDistance = Integer.MAX_VALUE;
        Node nn = null;
        for (Bucket bucket : routingTable.getBuckets()) {
            if (bucket.getNodes().size() > 0) {
                if (Math.abs(bucket.getBucketId() - bucketId) < closestDistance) {
                    closestDistance = Math.abs(bucket.getBucketId() - bucketId);
                    closest = bucket.getBucketId();
                    nn = bucket.getNodes().first();
                }
            }
        }
        System.out.println("closest" + closest);
        System.out.println("minha " + routingTable.getBucketId(key) + " ");
        if (routingTable.getBucketId(key) < closest || closest == Integer.MAX_VALUE) {
            if (storage.get(key) != null) {
                if (message.getType() == FIND_VALUE) {
                    message.setContent(Util.serializeBlock(storage.get(key)));
                    message.setType(FIND_VALUE_REPLY);
                } else {
                    message.setContent(Util.serializeNode(localNode));
                    message.setType(FIND_NODE_REPLY);
                }
                message.setLocalNode(Util.serializeNode(localNode));
                communicationInterface.send(receiver, message);
            } else {
                message.setType(SHOW);
                message.setContent(("Value not found \n").getBytes(StandardCharsets.UTF_8));
                message.setLocalNode(Util.serializeNode(localNode));
                communicationInterface.send(receiver, message);
            }

        }
        if (closest != Integer.MAX_VALUE) {

            for (Node node : routingTable.getBuckets()[closest].getNodes()) {
                this.communicationInterface.send(Util.serializeMessage(message), node.getAddr());
            }
            if (routingTable.getBucketId(nn.getId()) == closest) {
                if (storage.get(key) != null || (message.getType() == FIND_NODE)) {
                    if (routingTable.getBucketId(localNode.getId()) == closest) {
                        if (message.getType() == FIND_VALUE) {
                            message.setContent(Util.serializeBlock(storage.get(key)));
                            message.setType(FIND_VALUE_REPLY);
                        } else {
                            message.setContent(Util.serializeNode(localNode));
                            message.setType(FIND_NODE_REPLY);
                        }
                        message.setLocalNode(Util.serializeNode(localNode));
                        communicationInterface.send(receiver, message);
                    }
                } else {
                    message.setType(SHOW);
                    message.setContent(("Value not found \n").getBytes(StandardCharsets.UTF_8));
                    message.setLocalNode(Util.serializeNode(localNode));
                    communicationInterface.send(receiver, message);
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
                full.flip();
                Block block;
                Node rn;
                Key key;
                full.get(m);
                KademliaMessage message = Util.deserializeMessage(m);
                InetSocketAddress receiver = (Util.deserializeNode(message.getLocalNode())).getAddr();

                routingTable.addNode(Util.deserializeNode(message.getLocalNode()));

                switch (message.getType()) {
                    case PING:
                        if (socketChannel != null) {
                            socketChannel.write(ByteBuffer.wrap(("PINGED+\n").getBytes(StandardCharsets.UTF_8)));

                        }
                        message.setType(PING_REPLY);
                        message.setLocalNode(Util.serializeNode(this.localNode));
                        this.communicationInterface.send(Util.serializeMessage(message), receiver);
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
                            socketChannel.write(ByteBuffer.wrap(("Found \n" + rn + "\n" +
                                    block.toString()).getBytes(StandardCharsets.UTF_8)));

                        }
                        lastSearchedBlock = Util.deserializeBlock(message.getContent());

                        break;
                    case FIND_NODE_REPLY:
                        rn = Util.deserializeNode(message.getLocalNode());
                        if (socketChannel != null) {
                            socketChannel.write(ByteBuffer
                                    .wrap(("Found \n" + rn.toString()).getBytes(StandardCharsets.UTF_8)));
                        }
                        lastSearchedNode = Util.deserializeNode(message.getContent());

                        break;
                    case PING_REPLY:

                        callers.put(Util.deserializeNode(message.getLocalNode()).getId(),
                                Util.deserializeNode(message.getLocalNode()));
                        message.setType(STOP);
                        if (socketChannel != null)
                            socketChannel.write(ByteBuffer
                                    .wrap(("Receiving Ping from \n" + localNode.toString())
                                            .getBytes(StandardCharsets.UTF_8)));
                        message.setLocalNode(Util.serializeNode(this.localNode));

                        this.communicationInterface.send(Util.serializeMessage(message), receiver);
                        break;
                    case STOP:
                        if (socketChannel != null)
                            socketChannel
                                    .write(ByteBuffer.wrap(("Ping was replyed \n").getBytes(StandardCharsets.UTF_8)));
                        break;
                    case SHOW:
                        if (socketChannel != null)
                            socketChannel
                                    .write(ByteBuffer.wrap(message.getContent()));
                        break;

                    default:
                        if (socketChannel != null)
                            socketChannel
                                    .write(ByteBuffer.wrap(("Unexpected value:\n"
                                            + message.getType()).getBytes(StandardCharsets.UTF_8)));

                        throw new IllegalStateException("Unexpected value: " + message.getType());

                }
            } catch (Exception e) {
                e.printStackTrace();

            }

        }

    }

}
