package blockchain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Item {
  private String title;
  private String description;
  private float duration;
  private double price;

  @Override
  public String toString() {
    return (
      "|" + title + "//" + description + "//" + duration + "//" + price + "|"
    );
  }
}
