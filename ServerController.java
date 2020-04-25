import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class ServerController {

    Map<String, User> clients;

    public ServerController() {
        clients = new HashMap<>();
    }

    public boolean addClient(InetAddress address, Integer port, String name) {
        User client = new User(address, port, name);
        if (this.clients.containsKey(client.getClientKey())) {
            return false;
        } else {
            this.clients.put(client.getClientKey(), client);
            return true;
        }
    }

    public void registerHeartbeat(InetAddress address, Integer port, String name) {
        User user = new User(address, port, name);
        if (this.clients.containsKey(user.getClientKey())) {
            this.clients.get(user.getClientKey()).lastHeartbeat = LocalDateTime.now();
        } else {
            this.clients.put(user.getClientKey(), user);
        }
    }

    public Collection<User> getUsers() {
        return this.clients.values();
    }

    public void removeOldUsers() {
        int delay = 10;
        this.clients.values().removeIf(u -> u.lastHeartbeat.isBefore(LocalDateTime.now().minusSeconds(delay)));
    }
}
