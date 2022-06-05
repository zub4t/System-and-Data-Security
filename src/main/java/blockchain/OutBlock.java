package blockchain;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import p2p.Key;
import p2p.protocol.InfectionMessage;
import util.Util;

@Builder
@Getter
@Setter
public class OutBlock implements Serializable, Block, Comparable<OutBlock> {
  private Key winner;
  private InBlock inBlock;
  private String hash;
  public Key key;
  public boolean confimerdKey;

  @Override
  public String toString() {
    String str = "*****************************\n\n\n";
    str += "Block KEY:\t" + key + "\n";
    str += "Block HASH:\t" + hash + "\n";
    str += "Winner Key:\t" + winner + "\n";
    str += "\t**** In Block Content ***\n";
    str +=
      "TimeStamp:" + (new Timestamp(inBlock.getTimeStamp()).toString()) + "\n";
    str += "Digital signature:" + inBlock.getDigitalSignature() + "\n";
    str += "Previous Hash:" + inBlock.getPreviousHash() + "\n";

    InfectionMessage msg;
    String mssg;
    switch (inBlock.getContentType()) {
      case "InfectionMessage toSell":
        str +=
          "CotentType:\t" + "Sale announcement for the following items" + "\n";

        msg =
          (InfectionMessage) Util.convertBytesToObject(inBlock.getContent());
        str += "CotentType:\n";
        for (String item : msg.getToSell()) {
          str += "\t" + item + "\n";
        }
        str += "From:\t" + inBlock.getFrom() + "\n";
        break;
      case "InfectionMessage toBuy":
        str +=
          "CotentType:\t" + "Subscription for the following items" + "\n";

        msg =
          (InfectionMessage) Util.convertBytesToObject(inBlock.getContent());
        str += "Cotent:\n";
        for (String item : msg.getToBuy()) {
          str += "\t" + item + "\n";
        }
        str += "From:\t" + inBlock.getFrom() + "\n";

        break;
      case "Item:Sell of an item":
        Item item = (Item) Util.convertBytesToObject(inBlock.getContent());
        str += "CotentType:\t" + "Transaction of an item" + "\n";
        str += "To:\t" + inBlock.getTo() + "\n";
        str += "From:\t" + inBlock.getFrom() + "\n";
        str += "Title:\t" + item.getTitle() + "\n";
        str += "Tag:\t" + item.getTag() + "\n";
        str += "Description:\t" + item.getDescription() + "\n";
        str += "Price:\t" + item.getPrice() + "\n";

        break;
      case "BID":
        str += "CotentType:\t" + "BID" + "\n";

        mssg = (String) Util.convertBytesToObject(inBlock.getContent());
        str += "Cotent:\t" + mssg + "\n";
        break;
      case "init":
        str += "CotentType:\t" + "InitialBlock :D" + "\n";

        mssg = (String) Util.convertBytesToObject(inBlock.getContent());
        str += "Cotent:\t" + mssg + "\n";

        break;
    }

    str += "***************************** \n\n\n";
    return str;
  }

  @Override
  public int compareTo(OutBlock o) {
    return this.getKey().compareTo(o.getKey());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OutBlock) {
      OutBlock o = (OutBlock) obj;
      return this.getKey().equals(o.getKey());
    }
    return false;
  }
}
