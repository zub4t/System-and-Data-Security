package p2p;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.net.InetSocketAddress;

@Data
@EqualsAndHashCode(of = {"id"})
@Builder
public class Node implements Comparable<Node>, Serializable {
    private Key id;
    private InetSocketAddress addr;
    private long lastSeen = System.currentTimeMillis();

    @Override
    public int compareTo(Node o) {
        if (this.equals(o)) {
            return 0;
        }
        return (this.lastSeen > o.lastSeen) ? 1 : -1;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", addr=" + addr.getHostString() + ":" + addr.getPort() +
                ", lastSeen=" + lastSeen +
                '}';
    }
}