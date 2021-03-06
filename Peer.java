import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;



public class Peer {
    InetAddress address;
    Integer port;
    String name;
    LocalDateTime lastHeartbeat;
    Map<String, String> peerFiles;

    public Peer(InetAddress address, Integer port, String name) {
        this.address = address;
        this.port = port;
        this.name = name;
        this.peerFiles = new HashMap<>();
        lastHeartbeat = LocalDateTime.now();
    }


    public static String getKey(InetAddress address, Integer port){
        return address.toString() + ":" + port.toString();
    }

    public void registerFile(String filehash, String filename){
        this.peerFiles.put(filehash, filename);
    }

    public List<String> searchFiles(String name) {
        List<String> resultFiles = new ArrayList<>();
        for (Map.Entry<String, String> file : this.peerFiles.entrySet()) {
            if (file.getValue().toLowerCase().indexOf(name.toLowerCase()) != -1){
                String filename = file.getValue();
                String filehash = file.getKey().toString();
                resultFiles.add("File: " + filename + "\n" + "Hash: " + filehash);
            }
        }
        return resultFiles;
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
        String filesString="";
        for (Map.Entry<String, String> file : this.peerFiles.entrySet()) {
            filesString += "File: " + file.getValue() + "\n";
            filesString += "Hash: " + file.getKey() + "\n";
        }

        return "address: " + address.getHostAddress() + ":" + port +
                "\nname: " + name +
                "\n\n" + filesString;
    }
}
