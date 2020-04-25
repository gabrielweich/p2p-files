import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class MessageListener extends Thread {
    DatagramSocket socket;
    Map<byte[], String> myFiles;

    public MessageListener(DatagramSocket socket, Map<byte[], String> myFiles) {
        this.socket = socket;
        this.myFiles = myFiles;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] texto = new byte[1024];
                // recebe datagrama
                DatagramPacket packet = new DatagramPacket(texto, texto.length);
                socket.setSoTimeout(500);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println(packet.getAddress() + ": " + message + '\n');
                // if(message.startsWith("requestFile")) {
                //     this.requestedFile(message);
                // }
            } catch (IOException e) {
            }
        }
        System.out.println("Client finished.");
    }

    private void requestedFile(String message) {
        String[] tokens = message.split(";");
    }
}

class HeartbeatSender extends Thread {
    DatagramSocket socket;
    InetAddress address;
    Integer port;

    public HeartbeatSender(DatagramSocket socket, InetAddress address, Integer port) {
        this.socket = socket;
        this.address = address;
        this.port = port;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] texto = new byte[1024];
                texto = ("heartbeat").getBytes();
                DatagramPacket packet = new DatagramPacket(texto, texto.length, address, port);
                socket.send(packet);
                Thread.sleep(5000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Heartbeat finished.");
    }
}

public class ClientController {
    InetAddress serverAddress;
    int serverPort;
    String nickname;
    DatagramSocket socket;

    Map<byte[], String> myFiles;


    public ClientController(String serverAddress, String nickname) throws IOException {
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.nickname = nickname;
        this.serverPort = 4500;
        this.socket = new DatagramSocket();
        this.myFiles = new HashMap<>();
    }


    public void sendMessage(InetAddress address, Integer port, String message) {
        byte[] command = new byte[1024];
        command = message.getBytes();
        try {
            this.socket.send(new DatagramPacket(command, command.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetSocketAddress stringToIp(String string) throws URISyntaxException {
        URI uri = new URI("my://" + string);
        String host = uri.getHost();
        int port = uri.getPort();

        if (uri.getHost() == null || uri.getPort() == -1) {
            throw new URISyntaxException(uri.toString(), "URI must have host and port parts");
        }
        return new InetSocketAddress(host, port);
    }

    public void sendToCommand(String line) {
        String[] tokens = line.split(" ");
        if (tokens.length < 3) {
            System.out.println("Comando inválido, formato esperado: \"sendto <endereco>:<porta> <mensagem>\"");
            return;
        }
        try {
            InetSocketAddress address = this.stringToIp(tokens[1]);
            String message = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length));
            this.sendMessage(InetAddress.getByName(address.getHostName()), address.getPort(), message);
        } catch (UnknownHostException | URISyntaxException e) {
            System.out.println("Unable to send message to " + tokens[1]);
        }

    }

    

    private byte[] createFileHash(File file) throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[8192];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();

        byte[] hash = digest.digest();
        return hash;
    }

    private void registerFileCommand(String line) {
        String[] tokens = line.split(" ");
        File file = new File(tokens[1]);
        try {
            byte[] fileHash = this.createFileHash(file);
            this.myFiles.put(fileHash, file.getAbsolutePath());
            String message = "addfile;" + fileHash.toString() + ";" + file.getName();
            this.sendMessage(this.serverAddress, this.serverPort, message);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }   
    }

    private void searchFileCommand(String line) {
        String[] tokens = line.split(" ");
        String message = "search;" + tokens[1];
        this.sendMessage(this.serverAddress, this.serverPort, message);
    }

    private void resquestFileFrom(String line) {
        String[] tokens = line.split(" ");
        try {
            InetSocketAddress address = this.stringToIp(tokens[1]);
            String message = "requestFile;" + tokens[2];
            this.sendMessage(InetAddress.getByName(address.getHostName()), address.getPort(), message);
        } catch (UnknownHostException | URISyntaxException e) {
            System.out.println("Unable to send message to " + tokens[1]);
        }
    }

    public void start() {
        
		Thread messageListener = new MessageListener(this.socket, this.myFiles);
		Thread heartbeatSender = new HeartbeatSender(this.socket, this.serverAddress, this.serverPort);

		messageListener.start();
		heartbeatSender.start();


		String loginMessage = "login;" + this.nickname;
		this.sendMessage(this.serverAddress, serverPort, loginMessage);

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String line = scanner.nextLine();
			if (line.equals("exit")) break;
			else if (line.startsWith("clients")) this.sendMessage(this.serverAddress, serverPort, "clients");
            else if (line.startsWith("sendto")) this.sendToCommand(line);
            else if (line.startsWith("addfile")) this.registerFileCommand(line);
            else if (line.startsWith("search")) this.searchFileCommand(line);
            else if (line.startsWith("request")) this.resquestFileFrom(line);
            else {
                System.out.println("clients: ver os clientes registrados\n" +
            "sendto: enviar mensagem\n"+
            "addfile: registrar arquivo deste cliente no servidor\n"+
            "search: procurar arquivos com esta substring no servidor\n\n");
            }
		}

        scanner.close();
		messageListener.interrupt();
		heartbeatSender.interrupt();
    }
}