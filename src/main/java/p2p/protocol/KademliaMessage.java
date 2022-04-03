package p2p.protocol;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import util.Util;

import java.io.Serializable;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Getter
@Setter
@Builder(access = AccessLevel.PUBLIC)
public class KademliaMessage implements Serializable {
    private static SecureRandom random = new SecureRandom();


    private byte type;
    private byte[] seqNumber = new byte[8];
    private byte[] localNode = new byte[2048];
    private byte[] content = new byte[2048];



    public String getContentAsString(){
        return new String(this.content, StandardCharsets.UTF_8);
    }
    @Override
    public String toString() {
        String s ="";
        try {
            s= "KademliaMessage{" +
                    "type=" + type +
                    ", localNode=" +  Util.deserializeNode(localNode)+
                    ", seqNumber=" + Util.bytesToLong(seqNumber) +
                    ", content=" + new String(content, StandardCharsets.UTF_8) +
                    '}';
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }
}
