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

public class AdmServer {
    private Peer p;
    private SocketChannel sc;

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

        Option generateTeste = Option.builder("generateTeste").longOpt("generateTeste")
                .argName("generateTeste")
                .hasArg()
                .required(false)
                .desc("generateTeste").build();
        options.addOption(generateTeste);

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

        Option echo = Option.builder("echo").longOpt("echo")
                .argName("echo")
                .required(false)
                .hasArg()
                .desc("echo ")
                .build();
        options.addOption(echo);

        Option storeAll = Option.builder("sAll").longOpt("storeAll")
                .argName("storeAll")
                .required(false)
                .desc("set temporary blocks")
                .build();
        options.addOption(storeAll);

        Option connect = Option.builder("connect").longOpt("connect")
                .required(false)
                .argName("connect")
                .hasArg()
                .desc("connect to a web auction")
                .build();
        options.addOption(connect);

        Option sell = Option.builder("sell").longOpt("sell")
                .required(false)
                .argName("sell")
                .hasArg()
                .desc("sell an item")
                .build();
        options.addOption(sell);

        CommandLine cmd;
        CommandLineParser parser = new BasicParser();
        HelpFormatter helper = new HelpFormatter();

        Selector selector = Selector.open();

        ServerSocketChannel crunchifySocket = ServerSocketChannel.open();

        InetSocketAddress crunchifyAddr = new InetSocketAddress("localhost", Util.getRandomNumber(2000, 65535));
        System.out.println(crunchifyAddr);
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

                    SocketChannel socketChannel = (SocketChannel) myKey.channel();

                    ByteBuffer crunchifyBuffer = ByteBuffer.allocate(256);
                    socketChannel.read(crunchifyBuffer);
                    String rawInput = new String(crunchifyBuffer.array()).trim();

                    System.out.println(rawInput);
                    try {
                        cmd = parser.parse(options, rawInput.split(" "));
                        System.out.println(cmd.hasOption("lrt"));
                        if (cmd.hasOption("lrt")) {
                            socketChannel.write(ByteBuffer.wrap(p.listRoutingTable().getBytes(StandardCharsets.UTF_8)));
                        } else if (cmd.hasOption("s")) {
                            String value = cmd.getOptionValue("store");
                            if (p.tempStorage.get(Key.build(value)) != null) {
                                p.store(p.tempStorage.get(Key.build(value)));
                                p.tempStorage.remove(Key.build(value));
                            } else {
                                String r = ("the targeted key was not created yet, please use -g to create a new one");
                                socketChannel.write(ByteBuffer.wrap(r.getBytes(StandardCharsets.UTF_8)));

                            }
                        } else if (cmd.hasOption("fv")) {
                            String value = cmd.getOptionValue("findValue");
                            p.findValue(Key.build(value));
                        } else if (cmd.hasOption("g")) {
                            String value = cmd.getOptionValue("generate");
                            System.out.println("meu valor é");
                            System.out.println(value);
                            Block b = Miner.mineBlock(2, Block.builder()
                                    .data(value)
                                    .previousHash("")
                                    .timeStamp(System.currentTimeMillis())
                                    .build());
                            p.tempStorage.put(b.key, b);
                            socketChannel
                                    .write(ByteBuffer.wrap("new key generated \n".getBytes(StandardCharsets.UTF_8)));
                            socketChannel.write(
                                    ByteBuffer.wrap(b.key.getKey().toString(16).getBytes(StandardCharsets.UTF_8)));
                        } else if (cmd.hasOption("fn")) {
                            String value = cmd.getOptionValue("findNode");
                            p.findNode(Key.build(value));
                        } else if (cmd.hasOption("showLocalConfig")) {
                            String value = cmd.getOptionValue("showLocalConfig");
                            String r = ("config -> " + p.localNode);
                            socketChannel.write(ByteBuffer.wrap(r.getBytes(StandardCharsets.UTF_8)));
                        } else if (cmd.hasOption("ping")) {
                            String value = cmd.getOptionValue("ping");
                            System.out.println(value);
                            p.ping(value.split(":")[0], Integer.parseInt(value.split(":")[1]));

                        } else if (cmd.hasOption("ltb")) {
                            socketChannel.write(ByteBuffer.wrap(p.listTempBlocks().getBytes(StandardCharsets.UTF_8)));
                        } else if (cmd.hasOption("lsb")) {
                            socketChannel.write(ByteBuffer.wrap(p.listStoredBlocks().getBytes(StandardCharsets.UTF_8)));

                        } else if (cmd.hasOption("ss")) {
                            Peer.socketChannel = socketChannel;

                        } else if (cmd.hasOption("echo")) {
                            String value = cmd.getOptionValue("echo");
                            if (Peer.socketChannel == null) {
                                socketChannel.write(ByteBuffer
                                        .wrap("NO SocketChannel associated ".getBytes(StandardCharsets.UTF_8)));

                            } else {
                                Peer.socketChannel.write(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));

                            }
                        } else if (cmd.hasOption("generateTeste")) {
                            String value = cmd.getOptionValue("generateTeste");
                            for (int i = 0; i < Integer.parseInt(value); i++) {
                                Block b = Miner.mineBlock(2, Block.builder()
                                        .data(i + "")
                                        .previousHash("")
                                        .timeStamp(System.currentTimeMillis())
                                        .build());
                                p.tempStorage.put(b.key, b);
                            }
                            socketChannel.write(ByteBuffer.wrap("done".getBytes(StandardCharsets.UTF_8)));
                        } else if (cmd.hasOption("sAll")) {
                            for (Map.Entry<Key, Block> entry : p.tempStorage.entrySet()) {
                                p.store(entry.getValue());
                            }
                            p.tempStorage.clear();
                            socketChannel.write(ByteBuffer.wrap("Done".getBytes(StandardCharsets.UTF_8)));

                        } else if (cmd.hasOption("connect")) {
                            String value = cmd.getOptionValue("connect");
                            sc = SocketChannel.open();
                            sc.connect(
                                    new InetSocketAddress(value.split(":")[0], Integer.parseInt(
                                            value.split(":")[1])));
                        } else if (cmd.hasOption("sell")) {
                            String value = cmd.getOptionValue("sell");
                            p.findValue(Key.build(value));
                            if (p.lastSearchedBlock.getData().charAt(0) != 'I') {
                                socketChannel.write(ByteBuffer.wrap(
                                        "This block does not represent an item and is therefore not eligible for auction."
                                                .getBytes(StandardCharsets.UTF_8)));
                                return;
                            }
                            if (sc.isConnected()) {
                                socketChannel.write(ByteBuffer.wrap(("Advertise block with id  " + value
                                        + " for sale").getBytes(StandardCharsets.UTF_8)));
                                sc.write(ByteBuffer.wrap(p.lastSearchedBlock.getData().getBytes()));

                            } else {
                                socketChannel.write(ByteBuffer.wrap("The app is not connected to any web auction"
                                        .getBytes(StandardCharsets.UTF_8)));

                            }

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
