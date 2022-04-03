package blockchain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import p2p.Key;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Builder
@Getter
@Setter
public class Block implements Serializable {

    private static Logger logger = Logger.getLogger(Block.class.getName());

    private String hash = new String("");
    public Key key;
    private String previousHash;
    private String data;
    private long timeStamp;

    public static byte[] calculateBlockHash(Block block, long nonce) {
        String dataToHash = block.getPreviousHash() + Long.toString(block.getTimeStamp()) + Long.toString(nonce)
                + block.getData();
        MessageDigest digest = null;
        byte[] bytes = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            bytes = digest.digest(dataToHash.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        StringBuffer buffer = new StringBuffer();
        return bytes;
    }

    @Override
    public String toString() {
        return "Block ID:"+key.toString() + " Block data:" + this.data;
    }
}
