package util;

public class Constants {
  public static final byte PING = 0x00;
  public static final byte SHOW = 0x01;
  public static final byte FIND_NODE = 0x02;
  public static final byte INFECTION = 0x03;
  public static final byte FIND_VALUE = 0x04;
  public static final byte INFORM = 0x05;
  public static final byte STORE = 0x08;
  public static final byte PING_REPLY = 0x10;
  public static final byte STOP = 0x11;
  public static final byte FIND_VALUE_REPLY = 0x20;
  public static final byte FIND_NODE_REPLY = 0x40;
  public static final byte STORE_REPLY = 0x7F;
  public static int count = 0;
}
