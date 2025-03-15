public class Peer {
    private final String ip;
    private final int port;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", ip, port);
    }
} 