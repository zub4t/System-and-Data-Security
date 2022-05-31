package blockchain;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import p2p.Key;

@Builder
@Getter
@Setter
public class Block implements Serializable {
  private static Logger logger = Logger.getLogger(Block.class.getName());
  private String hash = new String("");
  public Key key;
  private String previousHash;
  private Key to;
  private Key from;
  private Key winner;
  private long timeStamp;
  private int seqNumber;
  private Item item;

  public String getData() {
    return (
      (to != null ? to : "to") +
      "->" +
      (from != null ? from : "from") +
      ":" +
      (winner != null ? winner : "winner") +
      "::" +
      (item != null ? item : "item")
    );
  }

  public static byte[] calculateBlockHash(Block block, long nonce) {
    String dataToHash =
      block.getPreviousHash() +
      Long.toString(block.getTimeStamp()) +
      Long.toString(nonce) +
      block.getData();
    MessageDigest digest = null;
    byte[] bytes = null;
    try {
      digest = MessageDigest.getInstance("SHA-1");
      bytes = digest.digest(dataToHash.getBytes("UTF-8"));
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
    StringBuffer buffer = new StringBuffer();
    block.setHash(new String(buffer));
    return bytes;
  }

  @Override
  public String toString() {
    return (
      "Block ID:" +
      (key != null ? key.toString() : "key") +
      " Block data:" +
      getData()
    );
  }
}
