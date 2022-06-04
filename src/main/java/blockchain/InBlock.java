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
public class InBlock implements Serializable, Block {
  private static Logger logger = Logger.getLogger(Block.class.getName());

  private String previousHash;
  private Key to;
  private Key from;
  private long timeStamp;
  private byte[] content;
  private String contentType;
  private byte[] digitalSignature;

  public String getData() {
    return (
      (to != null ? to : "to") + "->" + (from != null ? from : "from") + ":"
    );
  }

  public static byte[] calculateBlockHash(InBlock block, long nonce) {
    String TO = block.getTo() == null ? "" : block.getTo().toString();
    String FROM = block.getFrom() == null ? "" : block.getFrom().toString();
    String dataToHash =
      block.getPreviousHash() +
      "" +
      TO +
      "" +
      FROM +
      "" +
      block.getTimeStamp() +
      "" +
      block.getContent() +
      "" +
      block.getContentType() +
      "" +
      nonce;
    block.getDigitalSignature();
    MessageDigest digest = null;
    byte[] bytes = null;
    try {
      digest = MessageDigest.getInstance("SHA-1");
      bytes = digest.digest(dataToHash.getBytes("UTF-8"));
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }

    return bytes;
  }

  @Override
  public String toString() {
    return (" InBlock data:" + getData());
  }
}
