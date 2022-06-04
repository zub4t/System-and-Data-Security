package blockchain;

import p2p.Key;
import util.Util;

public class Miner {
  public static long nonce;

  public static OutBlock mineBlock(int prefix, InBlock block, Key key) {
    OutBlock outBlock = OutBlock.builder().winner(key).build();
    byte[] b;
    do {
       

      b = InBlock.calculateBlockHash(block, nonce);
      outBlock.key = new Key(b);
      outBlock.setHash(new String(b));
      outBlock.setInBlock(block);
      nonce++;
    } while (
      !(
        outBlock.getHash().charAt(0) == '0' &&
        Util.allCharactersSame(outBlock.getHash().substring(0, prefix))
      )
    );

    return outBlock;
  }
}
