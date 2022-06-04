package util;

import blockchain.Block;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import p2p.Key;
import p2p.Node;
import p2p.protocol.InfectionMessage;
import p2p.protocol.InterSystemMessage;
import p2p.protocol.KademliaMessage;

public class Util {
  private static byte[] b;

  public static byte[] intoToByte(int value) {
    byte[] bytes = new byte[Integer.BYTES];
    int length = bytes.length;
    for (int i = 0; i < length; i++) {
      bytes[length - i - 1] = (byte) (value & 0xFF);
      value >>= 8;
    }
    return bytes;
  }

  public static int ByteToInt(byte[] bytes) {
    int value = 0;
    for (byte b : bytes) {
      value = (value << 8) + (b & 0xFF);
    }
    return value;
  }

  public static byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }

  public static long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes);
    buffer.flip(); //need flip
    return buffer.getLong();
  }

  public static String xoring(String a, String b, int n) {
    String ans = "";
    for (int i = 0; i < n; i++) {
      if (a.charAt(i) == b.charAt(i)) ans += "0"; else ans += "1";
    }
    return ans;
  }

  public static byte[] serializeBlock(Block b) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(b);
      out.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new byte[0];
  }

  public static Block deserializeBlock(byte[] b) {
    Util.b = b;
    try {
      ByteArrayInputStream bb = new ByteArrayInputStream(b);
      ObjectInputStream o = new ObjectInputStream(bb);
      Block block = (Block) o.readObject();
      return block;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static byte[] serializeKey(Key k) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(k);
      out.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new byte[0];
  }

  public static Key deserializeKey(byte[] b) {
    try {
      ByteArrayInputStream bb = new ByteArrayInputStream(b);
      ObjectInputStream o = new ObjectInputStream(bb);
      Key key = (Key) o.readObject();
      return key;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String bytesToIP(byte[] b) {
    InetAddress addr = null;
    try {
      addr = InetAddress.getByAddress(b);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    String hostnameCanonical = addr.getCanonicalHostName();
    return hostnameCanonical;
  }

  public static byte[] serializeMessage(KademliaMessage message) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = null;
      out = new ObjectOutputStream(bos);
      out.writeObject(message);
      out.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new byte[0];
  }

  public static KademliaMessage deserializeMessage(byte[] b) {
    try {
      ByteArrayInputStream bb = new ByteArrayInputStream(b);
      ObjectInputStream o = new ObjectInputStream(bb);
      KademliaMessage message = (KademliaMessage) o.readObject();
      return message;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static byte[] serializeNode(Node n) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = null;
      out = new ObjectOutputStream(bos);
      out.writeObject(n);
      out.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new byte[0];
  }

  public static Node deserializeNode(byte[] b) {
    try {
      ByteArrayInputStream bb = new ByteArrayInputStream(b);
      ObjectInputStream o = new ObjectInputStream(bb);
      Node n = (Node) o.readObject();
      return n;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static int getRandomNumber(int min, int max) {
    return (int) ((Math.random() * (max - min)) + min);
  }

  public static boolean allCharactersSame(String s) {
    int n = s.length();
    for (int i = 1; i < n; i++) if (s.charAt(i) != s.charAt(0)) return false;

    return true;
  }

  public static byte[] serializeInterSystemMessage(InterSystemMessage msg) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(msg);
      out.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new byte[0];
  }

  public static InterSystemMessage deserializeInterSystemMessage(byte[] b) {
    try {
      ByteArrayInputStream bb = new ByteArrayInputStream(b);
      ObjectInputStream o = new ObjectInputStream(bb);
      InterSystemMessage msg = (InterSystemMessage) o.readObject();
      return msg;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static byte[] serializeInfectionMessage(InfectionMessage msg) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(msg);
      out.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new byte[0];
  }

  public static InfectionMessage deserializeInfectionMessage(byte[] b) {
    try {
      ByteArrayInputStream bb = new ByteArrayInputStream(b);
      ObjectInputStream o = new ObjectInputStream(bb);
      InfectionMessage msg = (InfectionMessage) o.readObject();
      return msg;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


   public static byte[] convertObjectToBytes(Object obj) {
      ByteArrayOutputStream boas = new ByteArrayOutputStream();
      try (ObjectOutputStream ois = new ObjectOutputStream(boas)) {
          ois.writeObject(obj);
          return boas.toByteArray();
      } catch (IOException ioe) {
          ioe.printStackTrace();
      }
      throw new RuntimeException();
  }

    public static Object convertBytesToObject(byte[] bytes) {
      InputStream is = new ByteArrayInputStream(bytes);
      try (ObjectInputStream ois = new ObjectInputStream(is)) {
          return ois.readObject();
      } catch (IOException | ClassNotFoundException ioe) {
          ioe.printStackTrace();
      }
      throw new RuntimeException();
  }
}
