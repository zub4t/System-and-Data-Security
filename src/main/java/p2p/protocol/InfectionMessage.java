package p2p.protocol;

import blockchain.Block;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import p2p.Key;

@Getter
@Setter
@Builder
public class InfectionMessage implements Serializable {
  private String OPERANTION;
  public long seqNumber;
  public List<String> toBuy;
  public List<String> toSell;
  public Set<Key> alreadySent;
  public Block block;

}
