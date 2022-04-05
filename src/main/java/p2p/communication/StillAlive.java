package p2p.communication;

public class StillAlive extends Exception {
    public StillAlive(String message) {
        super(message);
    }
}