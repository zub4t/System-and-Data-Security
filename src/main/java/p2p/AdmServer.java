package p2p;


import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import blockchain.Block;
import blockchain.Miner;
import org.apache.commons.cli.*;
import util.Util;

/**
 * @author Crunchify.com
 * Java NIO (Non-blocking I/O) with Server-Client Example - java.nio.ByteBuffer and channels.Selector
 * This is CrunchifyNIOServer.java
 */

public class AdmServer {
    private Peer p;


    AdmServer(Peer p) {

        this.p = p;
    }

    @SuppressWarnings("unused")
    public void start() throws IOException {
        Options options = new Options();

        Option generate = Option.builder("g").longOpt("generate")
                .argName("generate")
                .hasArg()
                .required(false)
                .desc("generate a new block").build();
        options.addOption(generate);

        Option store = Option.builder("s").longOpt("store")
                .argName("store")
                .hasArg()
                .required(false)
                .desc("Stores the value in the correct node").build();
        options.addOption(store);


        Option findValue = Option.builder("fv").longOpt("findValue")
                .argName("findValue")
                .hasArg()
                .required(false)
                .desc("find the value of the passed key").build();
        options.addOption(findValue);

        Option listRoutingTable = Option.builder("lrt").longOpt("listRoutingTable")
                .argName("listRoutingTable")
                .required(false)
                .desc("list all know nodes by buckets").build();
        options.addOption(listRoutingTable);


        Option ping = Option.builder("p").longOpt("ping")
                .argName("ping")
                .hasArg()
                .required(false)
                .desc("ping a node").build();
        options.addOption(ping);


        Option findNode = Option.builder("fn").longOpt("findNode")
                .argName("findNode")
                .hasArg()
                .required(false)
                .desc("find a node").build();
        options.addOption(findNode);


        Option showLocalConfig = Option.builder("show").longOpt("showLocalConfig")
                .argName("showLocalConfig")
                .required(false)
                .desc("show local configuration ").build();
        options.addOption(showLocalConfig);


        Option addWellKnowPeer = Option.builder("ap").longOpt("addWellKnowPeer")
                .argName("addWellKnowPeer")
                .required(false)
                .hasArg()
                .desc("add a peer manually ").build();
        options.addOption(addWellKnowPeer);

        Option listTempBlocks = Option.builder("ltb").longOpt("listTempBlocks")
                .argName("listTempBlocks")
                .required(false)
                .desc("list temporary blocks").build();
        options.addOption(listTempBlocks);

        Option listStoredBlocks = Option.builder("lsb").longOpt("listStoredBlocks")
                .argName("listStoredBlocks")
                .required(false)
                .desc("list stored blocks").build();
        options.addOption(listStoredBlocks);

        Option setSocket = Option.builder("ss").longOpt("setSocket")
                .argName("setSocket")
                .required(false)
                .desc("set socket for communication").build();
        options.addOption(setSocket);


        CommandLine cmd;
        CommandLineParser parser = new BasicParser();
        HelpFormatter helper = new HelpFormatter();


        // Selector: A multiplexor of SelectableChannel objects.
        // A selector may be created by invoking the open method of this class, which will use the system's default selector provider to create a new selector.
        // A selector may also be created by invoking the openSelector method of a custom selector provider. A selector remains open until it is closed via its close method.
        Selector selector = Selector.open(); // selector is open here

        // ServerSocketChannel: A selectable channel for stream-oriented listening sockets.
        // A server-socket channel is created by invoking the open method of this class.
        // It is not possible to create a channel for an arbitrary, pre-existing ServerSocket.
        ServerSocketChannel crunchifySocket = ServerSocketChannel.open();

        // InetSocketAddress: This class implements an IP Socket Address (IP address + port number) It can also be a pair (hostname + port number),
        // in which case an attempt will be made to resolve the hostname.
        // If resolution fails then the address is said to be unresolved but can still be used on some circumstances like connecting through a proxy.
        InetSocketAddress crunchifyAddr = new InetSocketAddress("localhost", Util.getRandomNumber(2000, 65535));
        System.out.println(crunchifyAddr);
        // Binds the channel's socket to a local address and configures the socket to listen for connections
        crunchifySocket.bind(crunchifyAddr);

        // Adjusts this channel's blocking mode.
        crunchifySocket.configureBlocking(false);

        int ops = crunchifySocket.validOps();

        // SelectionKey: A token representing the registration of a SelectableChannel with a Selector.
        // A selection key is created each time a channel is registered with a selector.
        // A key remains valid until it is cancelled by invoking its cancel method, by closing its channel, or by closing its selector.
        SelectionKey selectKy = crunchifySocket.register(selector, ops, null);

        // Infinite loop..
        // Keep server running
        log("I'm a server and I'm waiting for new connection and buffer select...");

        while (true) {

            // Selects a set of keys whose corresponding channels are ready for I/O operations
            selector.select();

            // token representing the registration of a SelectableChannel with a Selector
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey myKey = iterator.next();

                // Tests whether this key's channel is ready to accept a new socket connection
                if (myKey.isAcceptable()) {
                    SocketChannel socketChannel = crunchifySocket.accept();

                    // Adjusts this channel's blocking mode to false
                    socketChannel.configureBlocking(false);

                    // Operation-set bit for read operations
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    log("Connection Accepted: " + socketChannel.getLocalAddress() + "\n");

                    // Tests whether this key's channel is ready for reading
                } else if (myKey.isReadable()) {

                    SocketChannel socketChannel = (SocketChannel) myKey.channel();

                    // ByteBuffer: A byte buffer.
                    // This class defines six categories of operations upon byte buffers:
                    // Absolute and relative get and put methods that read and write single bytes;
                    // Absolute and relative bulk get methods that transfer contiguous sequences of bytes from this buffer into an array;
                    ByteBuffer crunchifyBuffer = ByteBuffer.allocate(256);
                    socketChannel.read(crunchifyBuffer);
                    String rawInput = new String(crunchifyBuffer.array()).trim();

                    System.out.println(rawInput);
                    try {
                        cmd = parser.parse(options, rawInput.split(" "));
                        System.out.println(cmd.hasOption("lrt"));
                        if (cmd.hasOption("lrt")) {
                            socketChannel.write(ByteBuffer.wrap(p.listRoutingTable().getBytes(StandardCharsets.UTF_8)));
                        }

                        if (cmd.hasOption("s")) {
                            String value = cmd.getOptionValue("store");
                            if (p.tempStorage.get(Key.build(value)) != null) {
                                p.store(p.tempStorage.get(Key.build(value)));
                                p.tempStorage.remove(Key.build(value));
                            } else {
                                String r = ("the targeted key was not created yet, please use -g to create a new one");
                                socketChannel.write(ByteBuffer.wrap(r.getBytes(StandardCharsets.UTF_8)));

                            }
                        }

                        if (cmd.hasOption("fv")) {
                            String value = cmd.getOptionValue("findValue");
                            p.findValue(Key.build(value));
                        }

                        if (cmd.hasOption("g")) {
                            String value = cmd.getOptionValue("generate");
                            Block b = Miner.mineBlock(0, Block.builder()
                                    .data(value)
                                    .previousHash("")
                                    .timeStamp(System.currentTimeMillis())
                                    .build());
                            p.tempStorage.put(b.key, b);
                            socketChannel.write(ByteBuffer.wrap("new key generated \n".getBytes(StandardCharsets.UTF_8)));
                            socketChannel.write(ByteBuffer.wrap(b.key.getKey().toString(16).getBytes(StandardCharsets.UTF_8)));
                        }


                        if (cmd.hasOption("fn")) {
                            String value = cmd.getOptionValue("findNode");
                            p.findNode(Key.build(value));
                        }

                        if (cmd.hasOption("showLocalConfig")) {
                            String value = cmd.getOptionValue("showLocalConfig");
                            String r = ("config -> " + p.localNode);
                            socketChannel.write(ByteBuffer.wrap(r.getBytes(StandardCharsets.UTF_8)));
                        }
                        if (cmd.hasOption("ping")) {
                            String value = cmd.getOptionValue("ping");
                            System.out.println(value);
                            p.ping(value.split(":")[0], Integer.parseInt(value.split(":")[1]));

                        }

                        if (cmd.hasOption("ap")) {
                            System.out.println("AP");
                            String value = cmd.getOptionValue("addWellKnowPeer");
                            System.out.println(value);
                            p.addWellKnowPeer(value.split(":")[0], value.split(":")[1], Integer.parseInt(value.split(":")[2]));

                        }
                        if (cmd.hasOption("ltb")) {
                            socketChannel.write(ByteBuffer.wrap(p.listTempBlocks().getBytes(StandardCharsets.UTF_8)));
                        }
                        if (cmd.hasOption("lsb")) {
                            socketChannel.write(ByteBuffer.wrap(p.listStoredBlocks().getBytes(StandardCharsets.UTF_8)));

                        }
                        if (cmd.hasOption("ss")) {
                            Peer.socketChannel = socketChannel;

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(e.getMessage());
                        // helper.printHelp("CLI:", options);
                        OutputStream out = new FileOutputStream("output.txt");
                        PrintWriter pw = new PrintWriter(out, true);
                        helper.printUsage(pw, 500, "MPM software--:\n", options);
                        InputStream in = new FileInputStream("output.txt");
                        String h = "";
                        int bc = in.read();
                        while (bc != -1) {
                            h += (char) bc;
                            bc = in.read();
                        }


                        socketChannel.write(ByteBuffer.wrap(h.getBytes(StandardCharsets.UTF_8)));

                    }
                    socketChannel.write(ByteBuffer.wrap("\n".getBytes(StandardCharsets.UTF_8)));

                }
                iterator.remove();
            }
        }
    }


    private static void log(String str) {

        System.out.println(str);
    }
}
