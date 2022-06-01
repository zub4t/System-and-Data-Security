package p2p.protocol;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import p2p.Node;
import util.Util;

@Getter
@Setter
@Builder(access = AccessLevel.PUBLIC)
public class KademliaMessage implements Serializable {
  private byte type;
  private Long seqNumber;
  private Node localNode;
  private byte[] content;
  private String action;
  private String actionData;

  public String getContentAsString() {
    return new String(this.content, StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    String s = "";
    try {
      s =
        "KademliaMessage{" +
        "type=" +
        type +
        ", localNode=" +
        (localNode) +
        ", seqNumber=" +
        (seqNumber) +
        ", content=" +
        new String(content, StandardCharsets.UTF_8) +
        '}';
    } catch (Exception e) {
      e.printStackTrace();
    }
    return s;
  }
}
