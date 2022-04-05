package blockchain;

import p2p.Key;

public class Miner {

    public static long nonce;

    public static Block mineBlock(int prefix, Block block) {

        byte[] b;
        String prefixString = new String(new char[prefix]).replace('\0', '0');
      //  do {
            b = Block.calculateBlockHash(block, nonce);
            nonce++;

      //  } while (!block.getHash().substring(0, 2).equals(prefixString));
        byte[] b_aux = new byte[Key.ID_LENGTH / 8];
        for (int i = 0; i < Key.ID_LENGTH / 8; i++) {
            b_aux[i] = b[i];
        }
        block.key = new Key(b_aux);
        block.setHash(b.toString());
        return block;
    }
}
