
package p2p.protocol;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 *
 * @author marco
 */
public class InterSystemMessage implements Serializable {
    public int type;
    public String content;
    public Timestamp lastMsg;

}
