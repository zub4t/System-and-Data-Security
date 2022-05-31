package blockchain;

import p2p.Key;
import util.Util;

public class Miner {
  public static long nonce;

  public static Block mineBlock(
    int prefix,
    Key to,
    Key from,
    Key winner,
    Item item,
    String previousHash
  ) {
    Block block = Block
      .builder()
      .to(to)
      .from(from)
      .winner(winner)
      .item(item)
      .previousHash(previousHash)
      .build();

    byte[] b;
    do {
      b = Block.calculateBlockHash(block, nonce);
      block.key = new Key(b);
      block.setHash(new String(b));
      nonce++;
    } while (
      !(
        block.getHash().charAt(0) == '0' &&
        Util.allCharactersSame(block.getHash().substring(0, prefix))
      )
    );

    return block;
  }

  public static void main(String[] args) {
    Block b = Miner.mineBlock(
      2,
      Key.random(),
      Key.random(),
      Key.random(),
      Item
        .builder()
        .title("teste")
        .price(0)
        .description("description")
        .duration(0)
        .build(),
      ""
    );

    System.out.println(b.getHash());
  }
}
