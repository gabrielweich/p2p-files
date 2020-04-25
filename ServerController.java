import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    public String searchFiles(String name) {
        String result = "";
        for (Peer peer : this.peers.values()){
            List<String> peerFiles = peer.searchFiles(name);
            if (peerFiles.size() > 0) {
                result += "\n\nPeer name: " + peer.name;
                result += "\nPeer host: " + peer.address.getHostAddress() + ":" + peer.port;
                for (String file : peer.searchFiles(name)) {
                    result += "\n" + file + "\n";
                }
                result += "\n";
            }
        }
        return result;
    }

    public void registerHeartbeat(String peerKey) {
        if (this.peers.containsKey(peerKey))
            this.peers.get(peerKey).lastHeartbeat = LocalDateTime.now();
    }

    public void registerFile(String peerKey, String filehash, String filename) {
        if (this.peers.containsKey(peerKey))
            this.peers.get(peerKey).registerFile(filehash, filename);
    }

    public Collection<Peer> getPeers() {
        return this.peers.values();
    }

    public void removeOldUsers() {
        int delay = 10;
        this.peers.values().removeIf(u -> u.lastHeartbeat.isBefore(LocalDateTime.now().minusSeconds(delay)));
    }
}
