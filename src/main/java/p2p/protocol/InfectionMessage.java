package p2p.protocol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import blockchain.Block;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Builder()
public class InfectionMessage implements Serializable{
  public long seqNumber; 
  public List<String> toBuy = new ArrayList<>();
  public List<String> toSell = new ArrayList<>();
  public Block block;
}
