import java.net.InetAddress;
import java.time.LocalDateTime;

public class Peer {
    InetAddress address;
    Integer port;
    String name;
    LocalDateTime lastHeartbeat;

    public Peer(InetAddress address, Integer port, String name) {
        this.address = address;
        this.port = port;
        this.name = name;
        lastHeartbeat = LocalDateTime.now();
    }


    public static String getKey(InetAddress address, Integer port){
        return address.toString() + ":" + port.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Peer))
            return false;

        Peer c = (Peer) o;
        return Peer.getKey(c.address, c.port).equals(Peer.getKey(this.address, this.port));
    }

    @Override
    public String toString() {
        return "address=" + address +
                "\nport=" + port +
                "\nname=" + name;
    }
}
