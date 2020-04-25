import java.net.InetAddress;
import java.time.LocalDateTime;

public class User {
    InetAddress address;
    Integer port;
    String name;
    LocalDateTime lastHeartbeat;

    public User(InetAddress address, Integer port, String name) {
        this.address = address;
        this.port = port;
        this.name = name;
        lastHeartbeat = LocalDateTime.now();
    }


    public String getClientKey() {
        return this.address.toString() + ":" + this.port.toString() + "/" + this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof User))
            return false;

        User c = (User) o;
        return c.getClientKey().equals(this.getClientKey());
    }

    @Override
    public String toString() {
        return "address=" + address +
                "\nport=" + port +
                "\nname=" + name;
    }
}
