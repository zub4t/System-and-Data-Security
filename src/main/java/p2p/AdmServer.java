package p2p;

import blockchain.Block;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.*;

import p2p.protocol.InterSystemMessage;
import p2p.routig.Bucket;
import util.Util;

public class AdmServer {
  private Peer p;

  AdmServer(Peer p) {
    this.p = p;
  }

  @SuppressWarnings("unused")
  public void start() throws IOException {
    Options options = new Options();

    Option action = Option
      .builder()
      .longOpt("action")
      .argName("action")
      .hasArg()
      .required(true)
      .desc("Action to perform")
      .build();
    options.addOption(action);

    Option owner = Option
      .builder()
      .longOpt("owner")
      .argName("owner")
      .hasArg()
      .required(false)
      .desc("Inform the owner key")
      .build();
    options.addOption(owner);

    Option buyer = Option
      .builder()
      .longOpt("buyer")
      .argName("buyer")
      .hasArg()
      .required(false)
      .desc("Inform the buyer key")
      .build();
    options.addOption(buyer);

    Option price = Option
      .builder()
      .longOpt("price")
      .argName("price")
      .hasArg()
      .required(false)
      .desc("Inform the price of an item")
      .build();
    options.addOption(price);

    Option lastBlock = Option
      .builder()
      .longOpt("lastBlock")
      .argName("lastBlock")
      .hasArg()
      .required(false)
      .desc("Last block key")
      .build();
    options.addOption(lastBlock);

    CommandLine cmd;
    CommandLineParser parser = new BasicParser();
    HelpFormatter helper = new HelpFormatter();

    Selector selector = Selector.open();

    ServerSocketChannel crunchifySocket = ServerSocketChannel.open();

    InetSocketAddress crunchifyAddr = new InetSocketAddress(
      "localhost",
      Util.getRandomNumber(2000, 65535)
    );
    log("listening " + crunchifyAddr);
    crunchifySocket.bind(crunchifyAddr);

    crunchifySocket.configureBlocking(false);

    int ops = crunchifySocket.validOps();

    SelectionKey selectKy = crunchifySocket.register(selector, ops, null);

    log("I'm a server and I'm waiting for new connection and buffer select...");

    while (true) {
      selector.select();

      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = selectedKeys.iterator();

      while (iterator.hasNext()) {
        SelectionKey myKey = iterator.next();

        if (myKey.isAcceptable()) {
          SocketChannel socketChannel = crunchifySocket.accept();
          socketChannel.configureBlocking(false);
          socketChannel.register(selector, SelectionKey.OP_READ);
          log("Connection Accepted: " + socketChannel.getLocalAddress() + "\n");
        } else if (myKey.isReadable()) {
          InterSystemMessage msg = new InterSystemMessage();
          SocketChannel socketChannel = (SocketChannel) myKey.channel();
          ByteBuffer crunchifyBuffer = ByteBuffer.allocate(256);
          socketChannel.read(crunchifyBuffer);
          String rawInput = new String(crunchifyBuffer.array()).trim();
          System.out.println(rawInput);
          try {
            cmd = parser.parse(options, rawInput.split(" "));

            if (cmd.hasOption("action")) {
              String value = cmd.getOptionValue("action");
              System.out.println("meu valor é");
              System.out.println(value);
              switch (value) {
                case "listRountingTable":
                  msg.type = 2;
                  msg.content = "";
                  for (Bucket bucket : p.routingTable.getBuckets()) {
                    msg.content += bucket.getBucketId() + "\n";
                    for (Node node : bucket.getNodes()) {
                      msg.content +=
                        "\t" +
                        node.getId() +
                        "--" +
                        node.getAddr().getHostName() +
                        ":" +
                        node.getAddr().getPort() +
                        "\n";
                    }
                  }
                  socketChannel.write(
                    ByteBuffer.wrap(Util.serializeInterSystemMessage(msg))
                  );
                  break;
                case "listLocalStored":
                  msg.type = 1;
                  msg.content = "";
                  for (Map.Entry<Key, Block> entry : p.storage.entrySet()) {
                    msg.content += entry.getKey().toString() + "\n";
                  }
                  socketChannel.write(
                    ByteBuffer.wrap(Util.serializeInterSystemMessage(msg))
                  );
                  break;
                case "generate":
                  if (
                    cmd.hasOption("owner") &&
                    cmd.hasOption("buyer") &&
                    cmd.hasOption("lastBlock") &&
                    cmd.hasOption("price")
                  ) {
                    String ownerValue = cmd.getOptionValue("owner");
                    String buyerValue = cmd.getOptionValue("buyer");
                    double priceValue = Double.parseDouble(
                      cmd.getOptionValue("price")
                    );
                    String lastBlockValue = cmd.getOptionValue("lastBlock");
                    /*
                    Block b = Miner.mineBlock(
                      2,
                      Key.build(buyerValue),
                      Key.build(ownerValue),
                      p.localNode.getId(),
                      priceValue,
                      lastBlockValue
                    );
                    System.out.println(
                      "Genereted block key " +
                      b.getKey() +
                      " and its hash " +
                      b.getHash()
                    );
                    p.store(b);
                    msg.type = 0;
                    msg.content = b.getHash();
                    socketChannel.write(
                      ByteBuffer.wrap(Util.serializeInterSystemMessage(msg))
                    );
                    */
                  }
                  break;
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        iterator.remove();
      }
    }
  }

  private static void log(String str) {
    System.out.println(str);
  }
}
