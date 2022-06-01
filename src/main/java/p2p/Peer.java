package p2p;

import static util.Constants.*;

import GUI.SimpleGUI;
import blockchain.Block;
import blockchain.Item;
import blockchain.Miner;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import p2p.communication.CommunicationInterface;
import p2p.protocol.InfectionMessage;
import p2p.protocol.KademliaMessage;
import p2p.routig.Bucket;
import p2p.routig.RoutingTable;
import util.Util;

public class Peer {
  public Node localNode;
  public Block previousBlock = null;
  private DatagramChannel inChannel;
  private Set<Long> kamdemliaMessagesAlreadyProcessed = new TreeSet<>();
  public RoutingTable routingTable;
  public HashMap<Key, Block> storage = new HashMap<>();
  public List<String> internalInterests = new ArrayList<>();
  public HashMap<Key, List<String>> externalInterests = new HashMap<>();
  public HashMap<Key, List<Item>> intersectionlInterests = new HashMap<>();
  public List<Long> infectionMsgs = new ArrayList<>();
  public static HashMap<Key, Node> callers = new HashMap<Key, Node>();
  public static SocketChannel socketChannel = null;
  public List<Item> toSell = new ArrayList<>();
  public CommunicationInterface communicationInterface;
  public Node lastSearchedNode;
  public Block lastSearchedBlock;
  public static SimpleGUI simpleGUI;

  Peer(Node localNode) {
    this.localNode = localNode;
    try {
      inChannel = DatagramChannel.open();
      InetSocketAddress iAdd = localNode.getAddr();
      inChannel.socket().bind(iAdd);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    Node localNode = new Node(
      Key.random(),
      new InetSocketAddress("localhost", Util.getRandomNumber(2001, 65535)),
      0
    );
    System.out.println("KEY:  " + localNode.getId());

    Peer p = new Peer(localNode);
    p.communicationInterface = new CommunicationInterface();
    p.routingTable = new RoutingTable(20, localNode.getId(), p);
    Thread service = p.startService();
    simpleGUI = new SimpleGUI(p);

    simpleGUI.start();
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
    p.ping("localhost", 2000);
    service.join();
  }

  public Thread startService() {
    return new Thread(
      () -> {
        try {
          postOffice();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    );
  }

  public void log(String txt) {
    if (simpleGUI != null) {
      simpleGUI.log(txt);
    } else {
      System.out.println(txt);
    }
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

  public String listToSell() {
    String str = "Blocks->\n";
    for (Item entry : this.toSell) {
      str += (entry + "\n");
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

  public String listInternalInterest() {
    String str = "Internal Interest->\n";
    for (String entry : this.internalInterests) {
      str += (entry + "\n");
    }
    return str;
  }

  public String listExternalInterests() {
    String str = "External Interests->\n";
    for (Map.Entry<Key, List<String>> entry : externalInterests.entrySet()) {
      str += (entry.getKey() + "\n");
      for (String s : entry.getValue()) {
        str += "\t" + (s + "\n");
      }
    }
    return str;
  }

  public String listIntersectionInterests() {
    String str = "Intersection Interests->\n";
    for (Map.Entry<Key, List<Item>> entry : intersectionlInterests.entrySet()) {
      // str += (entry.getKey() + "\n");
      for (Item item : entry.getValue()) {
        str += (item.getTitle() + "\n");
      }
    }
    return str;
  }

  public String listFormatedInterests() {
    String str = "";
    for (Map.Entry<Key, List<Item>> entry : intersectionlInterests.entrySet()) {
      for (Item item : entry.getValue()) {
        str += ("*--------------------------------------------*\n");
        str += (item.getTitle() + "\n");
        str += (item.getDescription() + "\n");
        str += (item.getPrice() + "\n");
        str += (item.getId() + "\n");
        str += ("*--------------------------------------------*\n");
      }
    }
    return str;
  }

  public Key findOwnerOf(UUID id) {
    for (Map.Entry<Key, List<Item>> entry : intersectionlInterests.entrySet()) {
      for (Item item : entry.getValue()) {
        log(item.getId() + " -- " + id);
        if (item.getId().toString().equals(id.toString())) {
          return entry.getKey();
        } else {
          log("não igual");
        }
      }
    }

    return null;
  }

  public void ping(String IP, int PORT) {
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(PING);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent("PING".getBytes());
      outMessage.setLocalNode((this.localNode));
      this.communicationInterface.send(
          Util.serializeMessage(outMessage),
          new InetSocketAddress(IP, PORT)
        );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void store(Block block) {
    log("storing block");
    log(block.toString());
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(STORE);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeBlock(block));
      outMessage.setLocalNode((this.localNode));
      _storage(block.key, outMessage, block);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void infection(InfectionMessage msg) {
    long seqNumber = new Random().nextLong();
    infectionMsgs.add(seqNumber);
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(INFECTION);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeInfectionMessage(msg));
      outMessage.setLocalNode((this.localNode));
      _infection(outMessage);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void findValue(Key key) {
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(FIND_VALUE);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeKey(key));
      outMessage.setLocalNode((this.localNode));
      _findNodeOrValue(key, outMessage, localNode.getAddr());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void findNode(Key key, String action, String data) {
    log("procurando key" + key.toString());
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(FIND_NODE);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent((Util.serializeKey(key)));
      outMessage.setLocalNode((this.localNode));
      outMessage.setAction(action);
      outMessage.setActionData(data);
      _findNodeOrValue(key, outMessage, localNode.getAddr());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void _storage(Key key, KademliaMessage message, Block block) {
    if (!kamdemliaMessagesAlreadyProcessed.contains(message.getSeqNumber())) {
      kamdemliaMessagesAlreadyProcessed.add(message.getSeqNumber());
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
      log("closest" + closest);
      log("minha " + routingTable.getBucketId(key) + " ");
      if (
        routingTable.getBucketId(key) <= closest || closest == Integer.MAX_VALUE
      ) {
        storage.put(block.getKey(), block);
      } else if (storage.get(key) == null && closest != Integer.MAX_VALUE) {
        for (Node node : routingTable.getBuckets()[closest].getNodes()) {
          this.communicationInterface.send(
              Util.serializeMessage(message),
              node.getAddr()
            );
        }
        if (routingTable.getBucketId(nn.getId()) == closest) {
          storage.put(block.getKey(), block);
        }
      }
    }
  }

  public void _infection(KademliaMessage message) {
    if (!kamdemliaMessagesAlreadyProcessed.contains(message.getSeqNumber())) {
      kamdemliaMessagesAlreadyProcessed.add(message.getSeqNumber());
      for (Bucket b : routingTable.getBuckets()) {
        for (Node node : b.getNodes()) {
          communicationInterface.send(node, message);
        }
      }
    }
  }

  public void _findNodeOrValue(
    Key key,
    KademliaMessage message,
    InetSocketAddress receiver
  ) {
    if (!kamdemliaMessagesAlreadyProcessed.contains(message.getSeqNumber())) {
      kamdemliaMessagesAlreadyProcessed.add(message.getSeqNumber());

      int bucketId = routingTable.getBucketId(key);
      int closest = Integer.MAX_VALUE;
      int closestDistance = Integer.MAX_VALUE;
      TreeSet<Node> nodes = null;
      for (Bucket bucket : routingTable.getBuckets()) {
        if (bucket.getNodes().size() > 0) {
          if (Math.abs(bucket.getBucketId() - bucketId) < closestDistance) {
            closestDistance = Math.abs(bucket.getBucketId() - bucketId);
            closest = bucket.getBucketId();
            nodes = bucket.getNodes();
          }
        }
      }
      log("closest" + closest);
      log("minha " + routingTable.getBucketId(key) + " ");
      if (
        routingTable.getBucketId(key) <= closest || closest == Integer.MAX_VALUE
      ) {
        if (message.getType() == FIND_VALUE) {
          if (storage.get(key) != null) {
            message.setContent(Util.serializeBlock(storage.get(key)));
            message.setType(FIND_VALUE_REPLY);
            message.setLocalNode((localNode));
            communicationInterface.send(receiver, message);
          } else {
            message.setType(SHOW);
            message.setContent(
              ("Value not found \n").getBytes(StandardCharsets.UTF_8)
            );
            message.setLocalNode((localNode));
            communicationInterface.send(receiver, message);
          }
        } else {
          if (!key.toString().equals(localNode.getId().toString())) {
            log("estão na mesma bucket entretanto quero o node em especifico");
            for (Node node : nodes) {
              if (!key.toString().equals(localNode.getId().toString())) {
                communicationInterface.send(nodes.first().getAddr(), message);
              }
            }
          } else {
            message.setContent(Util.serializeNode(localNode));
            message.setType(FIND_NODE_REPLY);
            message.setLocalNode((localNode));
            switch (message.getAction()) {
              case "BID":
                log("AQUIIIIIIIIIIIIIIIIIIIIIIIIIII BID");
                log(message.getActionData());
                UUID bidItemID = UUID.fromString(message.getActionData());
                for (Item item : toSell) {
                  if (item.getId().toString().equals(bidItemID.toString())) {
                    log("ENCONTRADO");
                    item.setLastBider((message.getLocalNode()).getId());
                    item.setPrice(item.getPrice() + (10.0 / nodes.size()));
                    message.setActionData(
                      message.getActionData() + ":" + item.getPrice()
                    );
                    log(item.toString());
                  }
                }
                message.setAction("updateItem");

                break;
            }
            communicationInterface.send(receiver, message);
          }
        }
      } else {
        communicationInterface.send(nodes.first().getAddr(), message);
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
        Node messageLocalNode = (message.getLocalNode());
        InetSocketAddress receiver = messageLocalNode.getAddr();

        routingTable.addNode(messageLocalNode);
        if (!localNode.equals(messageLocalNode)) {
          switch (message.getType()) {
            case INFECTION:
              boolean ctn = true;
              InfectionMessage msg = (InfectionMessage) Util.deserializeInfectionMessage(
                message.getContent()
              );

              for (Long s : infectionMsgs) {
                if (s == msg.seqNumber) {
                  log("Already propagated message");
                  ctn = false;
                }
              }
              if (ctn) {
                infectionMsgs.add(msg.seqNumber);
                if (msg.getBlock() != null) {
                  new Thread(
                    new Runnable() {

                      @Override
                      public void run() {
                        infection(msg);
                        if (msg.block.getWinner() == null) {
                          log("TRABALHANDO PRA REALIZAR O BLOCO");
                          msg.setBlock(
                            Miner.mineBlock(
                              2,
                              msg.getBlock(),
                              localNode.getId()
                            )
                          );
                          msg.setSeqNumber(new Random().nextLong());
                          log("BLOCO FINALIZADO");
                          infection(msg);
                        } else {
                          if (
                            previousBlock == null ||
                            !previousBlock
                              .getItem()
                              .getId()
                              .toString()
                              .equals(msg.block.getItem().toString())
                          ) {
                            log(
                              "node " +
                              messageLocalNode.getId() +
                              " win the run"
                            );
                            previousBlock = msg.block;
                            store(msg.block);
                          } else {
                            log(
                              "node " +
                              messageLocalNode.getId() +
                              " ñ conseguio a tempo"
                            );
                          }
                        }
                      }
                    }
                  )
                  .start();
                } else if (msg.toBuy != null || msg.toSell != null) {
                  if (msg.toBuy != null) {
                    //--------------------------------------------------------------//
                    //# Set node buy interests
                    List<String> ex = externalInterests.get(
                      messageLocalNode.getId()
                    );
                    if (ex == null) {
                      externalInterests.put(
                        messageLocalNode.getId(),
                        new ArrayList<String>()
                      );
                    }

                    ex = externalInterests.get(messageLocalNode.getId());

                    //------------------------------------------------------------//

                    for (String str : msg.toBuy) {
                      ex.add(str);
                    }
                    externalInterests.put(messageLocalNode.getId(), ex);
                  } else if (msg.toSell != null) {
                    //--------------------------------------------------------------//
                    //# Set node intersenction interests
                    List<Item> items = intersectionlInterests.get(
                      messageLocalNode.getId()
                    );

                    if (items == null) {
                      intersectionlInterests.put(
                        messageLocalNode.getId(),
                        new ArrayList<Item>()
                      );
                    }
                    items =
                      intersectionlInterests.get(messageLocalNode.getId());
                    //------------------------------------------------------------//

                    for (String sell : msg.toSell) {
                      for (String buy : internalInterests) {
                        if (
                          sell
                            .split(":")[1].trim()
                            .toUpperCase()
                            .equals(buy.trim().toUpperCase())
                        ) {
                          log(sell + "== " + buy);
                          items.add(
                            Item
                              .builder()
                              .id(UUID.fromString(sell.split(":")[0].trim()))
                              .title(sell.split(":")[1].trim().toUpperCase())
                              .description((sell.split(":")[2].trim()))
                              .price(
                                Double.parseDouble(sell.split(":")[3].trim())
                              )
                              .build()
                          );
                        } else {
                          log(sell + "!= " + buy);
                        }
                      }
                    }
                  }

                  _infection(message);
                }
              }

              break;
            case PING:
              if (socketChannel != null) {
                socketChannel.write(
                  ByteBuffer.wrap(
                    ("PINGED+\n").getBytes(StandardCharsets.UTF_8)
                  )
                );
              }
              message.setType(PING_REPLY);
              message.setLocalNode((this.localNode));
              this.communicationInterface.send(
                  Util.serializeMessage(message),
                  receiver
                );
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
              rn = messageLocalNode;
              block = Util.deserializeBlock(message.getContent());
              log("Found \n" + rn + "\n" + block.toString());
              lastSearchedBlock = Util.deserializeBlock(message.getContent());

              break;
            case FIND_NODE_REPLY:
              rn = messageLocalNode;
              log("Found \n" + rn);
              lastSearchedNode = Util.deserializeNode(message.getContent());
              switch (message.getAction()) {
                case "updateItem":
                  log("AQUIIIIIIIIIIIIIIIIIIIIIIIIIII updateItem");

                  UUID bidItemID = UUID.fromString(
                    message.getActionData().split(":")[0]
                  );
                  for (Map.Entry<Key, List<Item>> entry : intersectionlInterests.entrySet()) {
                    for (Item item : entry.getValue()) {
                      if (
                        item.getId().toString().equals(bidItemID.toString())
                      ) {
                        item.setPrice(
                          Double.parseDouble(
                            (message.getActionData().split(":")[1])
                          )
                        );
                      }
                    }
                  }

                  message.setAction("updateItem");
                  break;
              }
              break;
            case PING_REPLY:
              callers.put(messageLocalNode.getId(), messageLocalNode);
              message.setType(STOP);
              log("Receiving Ping from \n" + localNode.toString());
              message.setLocalNode((this.localNode));

              this.communicationInterface.send(
                  Util.serializeMessage(message),
                  receiver
                );
              break;
            case STOP:
              log("Ping was replyed \n");
              break;
            case SHOW:
              log(message.getContent().toString());

              break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
