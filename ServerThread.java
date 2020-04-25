import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class Command {
    InetAddress address;
    Integer port;
    String[] request;
    String peerKey;

    public Command(InetAddress address, int port, String[] request) {
        this.address = address;
        this.port = port;
        this.request = request;
        this.peerKey = Peer.getKey(address, port);
    }

    public String toString(){
        return address + ":" + port + "\nMessage: " + String.join(" ", this.request) + '\n';
    }
}

public class ServerThread extends Thread {
    protected DatagramSocket socket;
    protected String service;
    private ServerController controller;
    private volatile boolean running = true;
    private HashMap<String, Consumer<Command>> serverCommands;

    public ServerThread(String name) throws IOException {
        super(name);
        service = name;
        controller = new ServerController();
        socket = new DatagramSocket(Integer.parseInt(name));
        this.registerCommands();
        System.out.print("\nServer thread, port " + socket.getLocalPort());
    }

    private void registerCommands() {
        this.serverCommands = new HashMap<>();
        this.serverCommands.put("login", this::login);
        this.serverCommands.put("heartbeat", this::heartbeat);
        this.serverCommands.put("clients", this::getClients);
        this.serverCommands.put("addfile", this::registerFile);
        this.serverCommands.put("search", this::searchFiles);
    }


    private void login(Command command) {
        this.controller.addClient(command.address, command.port, command.request[1]);
        this.sendMessage(command.address, command.port, "Successfully registered");
    }

    private void heartbeat(Command command) {
        this.controller.registerHeartbeat(command.peerKey);
    }

    private void getClients(Command command){
        String message = this.controller.getPeers().stream().map(Peer::toString).collect(Collectors.joining("\n\n"));
        this.sendMessage(command.address, command.port, message);
    }

    private void registerFile(Command command) {
        String filehash = command.request[1];
        String filename = command.request[2];

        this.controller.registerFile(command.peerKey, filehash, filename);
        String message = "Arquivo registrado com sucesso!";
        this.sendMessage(command.address, command.port, message);
    }

    private void searchFiles(Command command) {
        String result = this.controller.searchFiles(command.request[1]);
        this.sendMessage(command.address, command.port, result);
    }

    private void shutdown() {
        running = false;
        socket.close();
        System.out.println("Finalizado...");
    }


    private void processPacket(DatagramPacket packet) {
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        String[] request = new String(packet.getData(), 0, packet.getLength()).split(";");
        Command command = new Command(address, port, request);

        System.out.println(command);

        if (this.serverCommands.containsKey(request[0])){
            this.serverCommands.get(request[0]).accept(command);
        }
        else {
            this.sendMessage(address, port, "Invalid command");
        }
    }

    private void sendMessage(InetAddress address, Integer port, String message){
        byte[] messageBytes = new byte[1024];
        messageBytes = message.getBytes();

        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        while (this.running) {
            try {
                this.controller.removeOldUsers();
                byte[] texto = new byte[1024];
                // recebe datagrama
                DatagramPacket packet = new DatagramPacket(texto, texto.length);
                socket.setSoTimeout(200);
                socket.receive(packet);

                this.processPacket(packet);

            } catch (IOException e) {
            }
        }
        System.out.println("Server shut down.");
    }

}
