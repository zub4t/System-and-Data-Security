package p2p;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;

@Data
@EqualsAndHashCode(of = "key")
public class Key implements Serializable {
    public final static int ID_LENGTH = 8;
    private BigInteger key;
    public Key(byte[] result) {
        if (result.length > ID_LENGTH / 8) {
            throw new RuntimeException("ID to long. Needs to be  " + ID_LENGTH + "bits long.");
        }
        this.key = new BigInteger(result);
    }

    public Key(BigInteger key) {
        if (key.toByteArray().length > ID_LENGTH / 8) {
            throw new RuntimeException("ID to long. Needs to be  " + ID_LENGTH + "bits long. Has "+key.toByteArray().length);
        }
        this.key = key;
    }

    public Key(int id) {
        this.key = BigInteger.valueOf(id);
    }

    public static Key random() {
        byte[] bytes = new byte[ID_LENGTH / 8];
        SecureRandom sr1 = new SecureRandom();
        sr1.nextBytes(bytes);
        return new Key(bytes);
    }
    public static Key build(String key) {
        return new Key(new BigInteger(key, 16));
    }

    public Key xor(Key nid) {
        return new Key(nid.getKey().xor(this.key));
    }
    public int getFirstSetBitIndex() {
        int prefixLength = 0;

        for (byte b : this.key.toByteArray()) {
            if (b == 0) {
                prefixLength += 8;
            } else {
                /* If the byte is not 0, we need to count how many MSBs are 0 */
                int count = 0;
                for (int i = 7; i >= 0; i--) {
                    boolean a = (b & (1 << i)) == 0;
                    if (a) {
                        count++;
                    } else {
                        break;   // Reset the count if we encounter a non-zero number
                    }
                }

                /* Add the count of MSB 0s to the prefix length */
                prefixLength += count;

                /* Break here since we've now covered the MSB 0s */
                break;
            }
        }
        return prefixLength;
    }

    @Override
    public String toString() {
        return this.key.toString(16);
    }

    public int getDistance(Key to) {
        /**
         * Compute the xor of this and to
         * Get the index i of the first set bit of the xor returned Key
         * The distance between them is ID_LENGTH - i
         */
        return ID_LENGTH - this.xor(to).getFirstSetBitIndex();
    }
}

