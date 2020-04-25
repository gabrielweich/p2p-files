import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class ServerController {

    Map<String, Peer> peers;

    public ServerController() {
        peers = new HashMap<>();
    }

    public boolean addClient(InetAddress address, Integer port, String name) {
        Peer peer = new Peer(address, port, name);
        String peerKey = Peer.getKey(address, port);

        if (this.peers.containsKey(peerKey)) {
            return false;
        } else {
            this.peers.put(peerKey, peer);
            return true;
        }
    }

    public void registerHeartbeat(InetAddress address, Integer port) {
        String peerKey = Peer.getKey(address, port);
        if (this.peers.containsKey(peerKey))
            this.peers.get(peerKey).lastHeartbeat = LocalDateTime.now();

    }

    public Collection<Peer> getPeers() {
        return this.peers.values();
    }

    public void removeOldUsers() {
        int delay = 10;
        this.peers.values().removeIf(u -> u.lastHeartbeat.isBefore(LocalDateTime.now().minusSeconds(delay)));
    }
}
