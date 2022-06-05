package p2p;

import static util.Constants.*;

import GUI.SimpleGUI;
import blockchain.Block;
import blockchain.InBlock;
import blockchain.Item;
import blockchain.Miner;
import blockchain.OutBlock;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.Cipher;
import javax.swing.JOptionPane;
import p2p.communication.CommunicationInterface;
import p2p.protocol.InfectionMessage;
import p2p.protocol.KademliaMessage;
import p2p.routig.Bucket;
import p2p.routig.RoutingTable;
import util.Util;

public class Peer {
  public OutBlock notConfirmedBlock;
  public Set<String> statusBlockMessages = new HashSet<>();
  public Set<String> alreadyInitiatedVoting = new HashSet<>();
  public Set<Key> callers = new HashSet<>();
  public boolean electionFinishAndPublishedWinner = false;
  /**********************************************/
  //MAPS
  public Map<Key, Log> InfectionLOG = new HashMap<>();
  public Map<Key, Block> storage = new HashMap<>();
  public Map<String, List<Node>> layers = new TreeMap<>();
  public Map<Key, List<String>> externalInterests = new HashMap<>();
  public Map<Key, List<Item>> intersectionlInterests = new HashMap<>();
  public Map<Long, Map<OutBlock, Integer>> electionsVotes = new TreeMap<>();
  public Map<Long, Boolean> hasElectionToProcess = new TreeMap<>();

  /**********************************************/
  //Lists
  public List<Long> infectionMsgs = new ArrayList<>();
  public List<Item> toSell = new ArrayList<>();
  public List<String> internalInterests = new ArrayList<>();
  public ArrayList<OutBlock> previousBlocks = new ArrayList<>();

  /**********************************************/
  //Lists
  /**********************************************/
  //cryptography stuffs
  public KeyPairGenerator generator;
  public KeyPair pair;
  public MessageDigest md;

  /**********************************************/
  // Communication
  public Node localNode;
  public DatagramChannel channel;
  private Set<Long> kamdemliaMessagesAlreadyProcessed = new TreeSet<>();
  public RoutingTable routingTable;
  public static SocketChannel socketChannel = null;
  public CommunicationInterface communicationInterface;
  /**********************************************/
  // graphical interface
  public static SimpleGUI simpleGUI;

  class Log {
    String OPERATION;
    Long timestamp;

    public Log(String OPERATION, Long timestamp) {
      this.OPERATION = OPERATION;
      this.timestamp = timestamp;
    }
  }

  Peer(Node localNode) {
    this.localNode = localNode;
    try {
      channel = DatagramChannel.open();
      InetSocketAddress iAdd = localNode.getAddr();
      channel.socket().bind(iAdd);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param args
   * @throws InterruptedException«
   */

  public static void main(String[] args)
    throws InterruptedException {
    System.out.println("End:"+args[0]);
    //**********************************************/
    //iniciando um peer com um end aleatorio
    Node localNode = new Node(
      Key.random(),
      new InetSocketAddress(
        args[0],
        Util.getRandomNumber(2001, 65535)
      ),
      0
    );
    Peer p = new Peer(localNode);
    p.routingTable = new RoutingTable(20, localNode.getId(), p);
    p.communicationInterface = new CommunicationInterface(p.channel);

    //**********************************************/
    ///setting up cryptography stuff
    try {
      p.generator = KeyPairGenerator.getInstance("RSA");
      p.md = MessageDigest.getInstance("SHA-256");
      p.generator.initialize(2048);
      p.pair = p.generator.generateKeyPair();
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    //**********************************************/
    //initing service to listening
    Thread service = p.startService();
    service.start();
    p.ping("34.175.136.67", 2000);

    //**********************************************/
    p.log("My KEY:  " + localNode.getId());

    //**********************************************/
    Timer timer = new Timer();
    timer.schedule(new Task(p), 1000, 5000);
    // initiating graphical
    simpleGUI = new SimpleGUI(p);
    simpleGUI.start();

    service.join();
  }

  //**************************************************************************************************************/
  //This block is dedicated to function regarding  relay or message generation
  // # relay
  // # generation

  /**
   * This function does a search for a key. which is stored in kamdemia. The value found is received asynchronous and
   *  is received into the postOffice of the peer who calls this function under the type of a FIND_VALUE_REPLY
   * @param key
   * The searched key
   */
  public void findValue(Key key) {
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(FIND_VALUE);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeKey(key));
      outMessage.setLocalNode((this.localNode));
      _find(key, outMessage, localNode.getAddr());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *  This function does a search for a key. which is stored in kamdemia. The node when is found is reported as
   *  a FOUND_NODE_REPLY message in the postOffice
   * @param key
   * The searched key
   * @param action
   * - Not mandatory - The action to made after found the node
   * @param data
   * - Not mandatory - Some information that is needed to perform the action
   */
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
      _find(key, outMessage, localNode.getAddr());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This function sends a message to all nodes on the kademlia network
   * @param message
   * The message that is intended for all nodes in the kademlia network
   */
  public void infection(InfectionMessage msg) {
    System.out.println(
      "INICIANDO CADEIA INFECTION" +
      msg.getSeqNumber() +
      " com operação " +
      msg.getOPERANTION() +
      "\n"
    );

    long seqNumber = new Random().nextLong();
    infectionMsgs.add(seqNumber);
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(INFECTION);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeInfectionMessage(msg));
      outMessage.setLocalNode((localNode));
      _infection(outMessage);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void overLayer(InfectionMessage msg, String tag) {
    long seqNumber = new Random().nextLong();
    infectionMsgs.add(seqNumber);
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(INFECTION);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeInfectionMessage(msg));
      outMessage.setLocalNode((this.localNode));
      _overLayer(outMessage, tag);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param IP
   * @param PORT
   */
  public void ping(String IP, int PORT) {
    log("Ping to " + IP + " " + PORT);
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(PING);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent("PING".getBytes());
      outMessage.setLocalNode((this.localNode));
      communicationInterface.send(
        Util.serializeMessage(outMessage),
        new InetSocketAddress(IP, PORT)
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void ping(Node no) {
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(PING);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent("PING".getBytes());
      outMessage.setLocalNode((this.localNode));
      communicationInterface.send(
        Util.serializeMessage(outMessage),
        no.getAddr()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param outblock
   */
  public void store(OutBlock outblock) {
    long seqNumber = new Random().nextLong();
    KademliaMessage outMessage = KademliaMessage.builder().build();
    try {
      outMessage.setType(STORE);
      outMessage.setSeqNumber((seqNumber));
      outMessage.setContent(Util.serializeBlock(outblock));
      outMessage.setLocalNode((this.localNode));
      _find(outblock.key, outMessage, localNode.getAddr());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void _infection(KademliaMessage message) {
    InfectionMessage msg = (InfectionMessage) Util.convertBytesToObject(
      message.getContent()
    );

    Set<Key> msgKey = new TreeSet<>();
    Set<Key> alreadySent = new TreeSet<>();
    alreadySent.add(localNode.getId());
    if (msg.getAlreadySent() != null) {
      for (Key key : msg.getAlreadySent()) {
        alreadySent.add(key);
        msgKey.add(key);
      }
    }

    for (Key key : getAllKnowKeys()) {
      alreadySent.add(key);
    }

    msg.setAlreadySent(alreadySent);
    message.setContent(Util.convertObjectToBytes(msg));
    int c = 0;
    if (!kamdemliaMessagesAlreadyProcessed.contains(message.getSeqNumber())) {
      kamdemliaMessagesAlreadyProcessed.add(message.getSeqNumber());
      for (Bucket b : routingTable.getBuckets()) {
        for (Node node : b.getNodes()) {
          boolean send = true;

          for (Key key : msgKey) {
            if (node.getId().toString().equals(key.toString())) {
              send = false;
            }
          }

          if (send) {
            c++;
            communicationInterface.send(node, message);
          }
        }
      }
    }
    //System.out.println("MSG INFECTION" + msg.getSeqNumber() + " ENVIADA PARA " + c + " nodes");
  }

  private void _overLayer(KademliaMessage message, String tag) {
    for (Map.Entry<String, List<Node>> entry : layers.entrySet()) {
      for (Node node : entry.getValue()) {
        if (entry.getKey() == tag) {
          communicationInterface.send(node, message);
        }
      }
    }
  }

  private void _find(
    Key key,
    KademliaMessage message,
    InetSocketAddress receiver
  ) {
    Key lastBider = message.getLocalNode().getId();
    if (!kamdemliaMessagesAlreadyProcessed.contains(message.getSeqNumber())) {
      kamdemliaMessagesAlreadyProcessed.add(message.getSeqNumber());

      int myDistance = routingTable.getBucketId(key);
      int closest = Integer.MAX_VALUE;
      int closestDistance = Integer.MAX_VALUE;
      TreeSet<Node> nodes = null;

      for (Bucket bucket : routingTable.getBuckets()) {
        for (Node no : bucket.getNodes()) {
          int canditateDistance = no.getId().getDistance(key) - 1;

          if (canditateDistance == -1) {
            canditateDistance = 0;
          }

          if (canditateDistance < closestDistance) {
            closestDistance = canditateDistance;
            closest = bucket.getBucketId();
            nodes = bucket.getNodes();
            //System.out.println(nodes.last().getId());
          }
        }
      }

      //System.out.println("bucketId " + myDistance);
      //System.out.println("closest to key " + closest);

      if (myDistance <= closest || closest == Integer.MAX_VALUE) {
        if (message.getType() == FIND_VALUE || message.getType() == STORE) {
          // Handle FIND_VALUE or STORE case
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
            OutBlock outBlock = (OutBlock) Util.convertBytesToObject(
              message.getContent()
            );
            storage.put(outBlock.getKey(), outBlock);

            if (
              !previousBlocks
                .get(previousBlocks.size() - 1)
                .getInBlock()
                .getPreviousHash()
                .equals(outBlock.getInBlock().getPreviousHash())
            ) {
              previousBlocks.add(outBlock);
            } else {}
          }
          log("Sending STORE_REPLY For node " + message.getLocalNode().getId());
          message.setType(STORE_REPLY);
          message.setLocalNode((localNode));
          communicationInterface.send(receiver, message);
        } else {
          // Handle FIND_NODE case
          if (!key.toString().equals(localNode.getId().toString())) {
            for (Node node : nodes) {
              if (!key.toString().equals(localNode.getId().toString())) {
                communicationInterface.send(node.getAddr(), message);
              }
            }
          } else {
            switch (message.getAction()) {
              case "BID":
                log(message.getActionData());
                UUID bidItemID = UUID.fromString(message.getActionData());
                boolean exist = false;
                for (Item item : toSell) {
                  if (item.getId().toString().equals(bidItemID.toString())) {
                    exist = true;

                    item.setLastBider(lastBider);
                    item.setPrice(item.getPrice() + (10.0 / nodes.size()));
                    message.setActionData(
                      message.getActionData() + ":" + item.getPrice()
                    );
                    log(item.toString());
                  }
                }
                if (exist) {
                  message.setAction("updateItem");
                } else {
                  message.setAction("removeItem");
                }

                break;
              case "showAlertMessageBlockStatus":
                String messageBlockStatusCode = message
                  .getActionData()
                  .split("::")[0];
                String messageBlockStatusContent = message
                  .getActionData()
                  .split("::")[1];

                if (!statusBlockMessages.contains(messageBlockStatusCode)) {
                  statusBlockMessages.add(messageBlockStatusCode);
                  JOptionPane.showMessageDialog(
                    this.simpleGUI,
                    messageBlockStatusContent
                  );
                }

                break;
            }

            message.setContent(Util.serializeNode(localNode));
            message.setType(FIND_NODE_REPLY);
            message.setLocalNode((localNode));
            communicationInterface.send(receiver, message);
          }
        }
      } else {
        for (Node no : nodes) {
          communicationInterface.send(no, message);
        }
      }
    }
  }

  /**
   * @throws IOException
   */
  private void postOffice() throws IOException {
    Lock lockBlockpreviousHashReply = new ReentrantLock();

    while (true) {
      try {
        ByteBuffer full = ByteBuffer.allocate(65507);
        InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(
          full
        );
        byte[] m = new byte[full.position()];
        full.flip();
        OutBlock outBlock;
        Node rn;
        Key key;
        full.get(m);
        KademliaMessage message = Util.deserializeMessage(m);
        message.getLocalNode().setAddr(clientAddress);
        Node messageLocalNode = (message.getLocalNode());
        InetSocketAddress receiver = clientAddress;
        new Thread(
          new Runnable() {

            public void run() {
              routingTable.addNode(messageLocalNode);
            }
          }
        )
        .start();

        //
        //System.out.println(message.getType());
        if (true) {
          switch (message.getType()) {
            case INFORM:
              switch (message.getAction()) {
                case "previousHashReply":
                  outBlock =
                    (OutBlock) Util.convertBytesToObject(message.getContent());
                  blockTest(outBlock);
                  break;
              }

              break;
            case INFECTION:
              boolean ctn = true;
              InfectionMessage msg = (InfectionMessage) Util.deserializeInfectionMessage(
                message.getContent()
              );

              if (infectionMsgs.contains(msg.seqNumber)) {
                ctn = false;
              }

              if (ctn) {
                switch (msg.getOPERANTION()) {
                  case "toSell":
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
                      log("**************Item sales processing***************");
                      log(sell);
                      log("***********************************");
                      for (String buy : internalInterests) {
                        if (
                          sell
                            .split(":")[1].trim()
                            .toUpperCase()
                            .equals(buy.trim().toUpperCase())
                        ) {
                          items.add(
                            Item
                              .builder()
                              .id(UUID.fromString(sell.split(":")[0].trim()))
                              .tag((sell.split(":")[1].trim()))
                              .title(sell.split(":")[2].trim().toUpperCase())
                              .description((sell.split(":")[3].trim()))
                              .price(
                                Double.parseDouble(sell.split(":")[4].trim())
                              )
                              .build()
                          );
                        } else {}
                      }
                    }
                    break;
                  case "toBuy":
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
                    break;
                  case "blockGeneration":
                    InBlock inBlock = (InBlock) msg.getBlock();
                    generateOutBlock(inBlock);
                    break;
                  case "blockFinished":
                    //System.out.println("RECEBENDO MSG DE QUE ALGUEM TERMINOU");
                    outBlock = (OutBlock) msg.getBlock();
                    if (blockTest(outBlock)) {
                      log("**************New candidate block***************");
                      store(outBlock);
                    }
                    break;
                  case "previousHash":
                    OutBlock lstBlock = (OutBlock) getPreviousHash();

                    if (lstBlock != null) {
                      //System.out.println("RESPONDENDO PEDIDO DE PREVIOUS HASH");

                      KademliaMessage response = KademliaMessage
                        .builder()
                        .type(INFORM)
                        .seqNumber(new Random().nextLong())
                        .action("previousHashReply")
                        .content(Util.convertObjectToBytes(lstBlock))
                        .localNode((localNode))
                        .build();

                      communicationInterface.send(receiver, response);
                    }

                    break;
                  case "voteForPreviousHash":
                    new Thread(
                      new Runnable() {

                        @Override
                        public void run() {
                          OutBlock outBlock = (OutBlock) msg.getBlock();
                          boolean test = outBlock.equals(
                            previousBlocks.get(previousBlocks.size() - 1)
                          );

                          alreadyInitiatedVoting.add(
                            outBlock.getInBlock().getPreviousHash()
                          );
                          lockBlockpreviousHashReply.lock();
                          int current = 0;
                          if (electionsVotes.get(msg.getSeqNumber()) != null) {
                            if (
                              electionsVotes
                                .get(msg.getSeqNumber())
                                .get(outBlock) !=
                              null
                            ) {
                              current =
                                electionsVotes
                                  .get(msg.getSeqNumber())
                                  .get(outBlock);
                            } else {
                              electionsVotes
                                .get(msg.getSeqNumber())
                                .put(outBlock, 0);
                            }
                          } else {
                            Map m = new TreeMap<>();
                            m.put(outBlock, 0);
                            electionsVotes.put(msg.getSeqNumber(), m);
                          }

                          electionsVotes
                            .get(msg.getSeqNumber())
                            .put(outBlock, (current + 1));
                          hasElectionToProcess.put(msg.getSeqNumber(), true);
                          lockBlockpreviousHashReply.unlock();
                        }
                      }
                    )
                    .start();

                    break;
                  case "electionWinner":
                    this.electionFinishAndPublishedWinner = true;
                    outBlock = (OutBlock) msg.getBlock();
                    previousBlocks.remove(previousBlocks.size() - 1);
                    previousBlocks.add(outBlock);
                    break;
                }
                infectionMsgs.add(msg.seqNumber);
                _infection(message);
              }
              break;
            case PING:
              message.setType(PING_REPLY);
              message.setLocalNode((this.localNode));
              System.out.println(
                "PING TO " + receiver + " ou seria " + clientAddress
              );
              this.communicationInterface.send(
                  Util.serializeMessage(message),
                  clientAddress
                );
              break;
            case FIND_NODE:
            case FIND_VALUE:
              key = Util.deserializeKey(message.getContent());
              _find(key, message, receiver);
              break;
            case STORE:
              outBlock =
                (OutBlock) Util.convertBytesToObject(message.getContent());
              _find(outBlock.key, message, receiver);
              break;
            case STORE_REPLY:
              //System.out.println("INIaaCIANDO PROCESSO DE ELEIÇÂO");
              outBlock =
                (OutBlock) Util.convertBytesToObject(message.getContent());
              voteForPreviousHash(outBlock.getInBlock().getPreviousHash());
              break;
            case FIND_VALUE_REPLY:
              rn = messageLocalNode;
              outBlock =
                (OutBlock) Util.convertBytesToObject(message.getContent());
              log("Found \n" + rn + "\n" + outBlock.toString());
              break;
            case FIND_NODE_REPLY:
              rn = messageLocalNode;
              log("Found \n" + rn);
              switch (message.getAction()) {
                case "removeItem":
                  UUID bidItemID = UUID.fromString(
                    message.getActionData().split(":")[0]
                  );
                  Key toRemoveKey = null;
                  Item toRemoveItem = null;
                  for (Map.Entry<Key, List<Item>> entry : intersectionlInterests.entrySet()) {
                    for (Item item : entry.getValue()) {
                      if (
                        item.getId().toString().equals(bidItemID.toString())
                      ) {
                        toRemoveKey = entry.getKey();
                        toRemoveItem = item;
                      }
                    }
                  }

                  intersectionlInterests.get(toRemoveKey).remove(toRemoveItem);
                  break;
                case "updateItem":
                  System.out.println(message.getActionData());
                  bidItemID =
                    UUID.fromString(message.getActionData().split(":")[0]);

                  Map<Key, List<Item>> toRemove = new TreeMap<>();
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

                  break;
              }
              break;
            case PING_REPLY:
              callers.add(messageLocalNode.getId());

              message.setType(STOP);
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

  //**************************************************************************************************************/
  //This block is dedicated to function regarding  listing
  // #listing
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

  /**
   * @return String
   */
  public String listToSell() {
    String str = "Items :";
    for (Item entry : this.toSell) {
      str += (entry + "\n");
    }
    return str;
  }

  /**
   * @return String
   */
  public String listStoredBlocks() {
    String str = "Blocks stored locally:\n";
    for (Map.Entry<Key, Block> entry : this.storage.entrySet()) {
      str += (entry.getValue().toString() + "\n");
    }
    return str;
  }

  /**
   * @return String
   */
  public String listChain() {
    String str = "My view:\n";
    for (OutBlock block : this.previousBlocks) {
      str += (block.toString() + "\n");
    }
    return str;
  }

  /**
   * @return String
   */
  public String listInternalInterest() {
    String str = "Internal Interest->\n";
    for (String entry : this.internalInterests) {
      str += (entry + "\n");
    }
    return str;
  }

  /**
   * @return String
   */
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

  /**
   * @return String
   */
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

  /**
   * @return String
   */
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

  //**************************************************************************************************************/
  //This block is dedicated to cryptography functions
  // #cripto
  public boolean verify(
    Object obj,
    byte[] encryptedMessageHash,
    PublicKey pubKey
  ) {
    Cipher cipher;

    try {
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.DECRYPT_MODE, pubKey);
      byte[] decryptedMessageHash = cipher.doFinal(encryptedMessageHash);
      byte[] messageHash = md.digest(Util.convertObjectToBytes(obj));
      if (java.util.Arrays.equals(messageHash, decryptedMessageHash)) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * @param obj
   * @return byte[]
   */
  public byte[] sing(Object obj) {
    Cipher cipher;
    if (obj != null) {
      try {
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pair.getPrivate());
        byte[] messageHash = md.digest(Util.convertObjectToBytes(obj));
        byte[] digitalSignature = cipher.doFinal(messageHash);
        return digitalSignature;
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    } else {
      return null;
    }
  }

  //**************************************************************************************************************/
  //This block is dedicated to utility functions
  // #utility
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

  /**
   * @param txt
   */
  public void log(String txt) {
    if (simpleGUI != null) {
      simpleGUI.log(txt);
    } else {
      System.out.println(txt);
    }
  }

  public OutBlock succBlockOfHash(String prevHash) {
    for (OutBlock block : previousBlocks) {
      if (block.getInBlock().getPreviousHash().equals(prevHash)) {
        return block;
      }
    }
    return null;
  }

  public void voteForPreviousHash(String prevHash) {
    Thread t = new Thread(
      new Runnable() {

        @Override
        public void run() {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          OutBlock block = succBlockOfHash(prevHash);
          log("*************Initiating voting**************");
          System.out.println("My vote is for block " + block.getKey() + " -- ");
          if (block != null) {
            if (!alreadyInitiatedVoting.contains(prevHash)) {
              alreadyInitiatedVoting.add(prevHash);
              InfectionMessage msg = InfectionMessage
                .builder()
                .OPERANTION("voteForPreviousHash")
                .block(block)
                .seqNumber(new Random().nextLong())
                .alreadySent(new TreeSet<>())
                .build();
              infection(msg);
            }
          }
        }
      }
    );
    t.start();
  }

  /**
   * @return Block
   */
  public OutBlock getPreviousHash() {
    if (
      previousBlocks.size() > 0 &&
      previousBlocks.get(previousBlocks.size() - 1) != null
    ) {
      return previousBlocks.get(previousBlocks.size() - 1);
    } else {
      log("requesting hash for know nodes");

      InfectionMessage msg = InfectionMessage
        .builder()
        .OPERANTION("previousHash")
        .seqNumber(new Random().nextLong())
        .alreadySent(new TreeSet<>())
        .build();
      infection(msg);
      return null;
    }
  }

  /**
   * @param id
   * @return Key
   */
  public Key findOwnerOf(UUID id) {
    for (Map.Entry<Key, List<Item>> entry : intersectionlInterests.entrySet()) {
      for (Item item : entry.getValue()) {
        log(item.getId() + " -- " + id);
        if (item.getId().toString().equals(id.toString())) {
          return entry.getKey();
        }
      }
    }

    return null;
  }

  /**
   * @param inBlock
   */
  public void generateOutBlock(InBlock inBlock) {
    Thread t = new Thread(
      new Runnable() {

        @Override
        public void run() {
          //System.out.println("INICIANDO BLOCK MINING");
          OutBlock outBlock = Miner.mineBlock(2, inBlock, localNode.getId());

          {
            //System.out.println("TERMINEI A TEMPO");

            InfectionMessage msg = InfectionMessage
              .builder()
              .OPERANTION("blockFinished")
              .block(outBlock)
              .alreadySent(new TreeSet<>())
              .seqNumber(new Random().nextLong())
              .build();
            infection(msg);
          }
        }
      }
    );
    t.start();
  }

  /**
   * @return Set<Key>
   */
  public Set<Key> getAllKnowKeys() {
    Set<Key> set = new TreeSet<>();
    for (Bucket b : routingTable.getBuckets()) {
      for (Node n : b.getNodes()) {
        set.add(n.getId());
      }
    }

    return set;
  }

  public synchronized boolean blockTest(OutBlock block) {
    if (previousBlocks.size() > 0) {
      Boolean step1 = new Timestamp(
        previousBlocks
          .get(previousBlocks.size() - 1)
          .getInBlock()
          .getTimeStamp()
      )
      .before(new Time(block.getInBlock().getTimeStamp()));

      Boolean step2 = block
        .getInBlock()
        .getPreviousHash()
        .equals(previousBlocks.get(previousBlocks.size() - 1).getHash());

      if ((step1) && (step2)) {
        previousBlocks.add(block);
      }

      return step1 && step2;
    }
    previousBlocks.add(block);

    return true;
  }
}

class Task extends TimerTask {
  Peer p;

  Task(Peer p) {
    this.p = p;
  }

  @Override
  public void run() {
    try {
      for (Map.Entry<Long, Boolean> entry : p.hasElectionToProcess.entrySet()) {
        if (entry.getValue()) {
          p.hasElectionToProcess.put(entry.getKey(), false);
          p.log("Processing voting " + entry.getKey());
          int largest = 0;
          OutBlock mostVoted = null;
          for (Map.Entry<OutBlock, Integer> election : p
            .electionsVotes.get(entry.getKey())
            .entrySet()) {
            if (election.getValue() > largest) {
              mostVoted = election.getKey();
            } else if (election.getValue() == largest) {
              if (
                mostVoted.getKey().compareTo(election.getKey().getKey()) > 0
              ) {
                mostVoted = election.getKey();
              }
            }
          }
          if (mostVoted != null) {
            p.log("voting completed. Block selected is" + mostVoted.getKey());

            mostVoted.setConfimerdKey(true);
            // p.previousBlocks.add(mostVoted);
            InfectionMessage msg = InfectionMessage
              .builder()
              .block(mostVoted)
              .OPERANTION("electionWinner")
              .alreadySent(new TreeSet<>())
              .seqNumber(new Random().nextLong())
              .build();
            p.infection(msg);
          } else {
            System.out.println("VOTAÇÂO FINALIZADA HASH BLOCOM NULL BLOCO");
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
