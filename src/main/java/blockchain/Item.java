package blockchain;

import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import p2p.Key;

@Builder
@Getter
@Setter
public class Item implements Serializable, Comparable<Item> {
  private String title;
  private String tag;
  private String description;
  private float duration;
  private double price;
  private UUID id;
  private boolean alreadyAdvertised;
  private long creationTime;
  private long endTime;
  private Key lastBider;

  @Override
  public String toString() {
    String str = "*****************************\n";
    str += "Title:\t" + title + "\n";
    str += "Description:\t" + description + "\n";
    str += "Duration:\t" + duration + "\n";
    str += "Price:\t" + price + " BitCona\n";
    str += "Already Advertised:\t" + alreadyAdvertised + "\n";
    str += "*****************************\n";
    return str;
  }

  @Override
  public int compareTo(Item o) {
    return this.id.compareTo(o.getId());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Item)) {
      return false;
    } else {
      Item oo = (Item) obj;
      return oo.id.equals(this.id);
    }
  }
}
