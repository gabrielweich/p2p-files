import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class MessageListener extends Thread {
    DatagramSocket socket;
    Map<String, String> myFiles;
    Thread fileReceiverThread;

    public MessageListener(DatagramSocket socket, Map<String, String> myFiles) {
        this.socket = socket;
        this.myFiles = myFiles;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] texto = new byte[1024];
                DatagramPacket packet = new DatagramPacket(texto, texto.length);
                socket.setSoTimeout(500);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message from: " + packet.getAddress());
                System.out.println(message + "\n");
                if (message.startsWith("requestFile")) this.processFileRequest(packet, message);
                if (message.startsWith("willSendFile")) this.processFileConfirmation(packet, message);
                if (message.startsWith("readyToReceive")) this.processFileSend(packet, message);
                if (message.startsWith("fileNotFound")) this.processFileNotFount(packet, message);
            } catch (IOException e) {
            }
        }
        System.out.println("Client finished.");
    }

    private void processFileRequest(DatagramPacket packet, String message) {
        String[] tokens = message.split(";");
        String filehash = tokens[1];

        InetAddress requesterAddress = packet.getAddress();
        Integer requesterPort = packet.getPort();

        if (this.myFiles.containsKey(filehash)) {
            File file = new File(this.myFiles.get(filehash));
            if (file.exists()) {
                this.sendMessage(requesterAddress, requesterPort,
                        "willSendFile;" + file.getName() + ";" + filehash.toString());
            } else {
                this.myFiles.remove(filehash);
                this.sendMessage(requesterAddress, requesterPort, "fileNotFound");
            }
        } else {
            this.sendMessage(requesterAddress, requesterPort, "fileNotFound");
        }
    }

    private void processFileConfirmation(DatagramPacket packet, String message) throws SocketException {
        String[] tokens = message.split(";");
        DatagramSocket receiveSocket = new DatagramSocket();
        InetAddress senderAddress = packet.getAddress();
        Integer senderPort = packet.getPort();
        String filename = tokens[1];
        String filehash = tokens[2];
        this.fileReceiverThread = new FileReceiver(receiveSocket, senderAddress, senderPort, filename);
        String returnMessage = "readyToReceive;" + filehash;
        this.fileReceiverThread.start();
        this.sendMessage(receiveSocket, senderAddress, senderPort, returnMessage);
    }

    private void processFileSend(DatagramPacket packet, String message) throws IOException {
        InputStream ios = null;
        String[] tokens = message.split(";");
        File file = new File(this.myFiles.get(tokens[1]));
        if (!file.exists()) return;
        InetAddress receiverAddress = packet.getAddress();
        Integer receiverPort = packet.getPort();

        try  {
            ios = new FileInputStream(file);
            int i = 0;

            do {
                byte[] buf = new byte[1024];
                i = ios.read(buf);
                DatagramPacket contentPacket = new DatagramPacket(buf, buf.length, receiverAddress, receiverPort);
                this.socket.send(contentPacket);
            } while (i != -1);

            System.out.println("fim");
        }finally {
            if (ios != null) ios.close();
        }
    }

    private void processFileNotFount(DatagramPacket packet, String message) {
        System.out.println("O arquivo não foi encontrado.");
    }

    public void sendMessage(InetAddress address, Integer port, String message) {
        this.sendMessage(this.socket, address, port, message);
    }

    public void sendMessage(DatagramSocket socket, InetAddress address, Integer port, String message) {
        byte[] command = new byte[1024];
        command = message.getBytes();
        try {
            socket.send(new DatagramPacket(command, command.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class FileReceiver extends Thread {
    DatagramSocket socket;
    InetAddress senderAddress;
    Integer senderPort;
    String filename;

    public FileReceiver(DatagramSocket socket, InetAddress senderAddress, Integer senderPort, String filename) throws SocketException {
        this.socket = socket;
        this.senderPort = senderPort;
        this.filename = filename;
    }

    public void run() {
        try {
            FileOutputStream fos = new FileOutputStream(new File("files", this.filename));
            byte[] receiveData = new byte[1024];
            while (!Thread.currentThread().isInterrupted() && receiveData != null) {
                try {
                    DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                    socket.setSoTimeout(1000);
                    socket.receive(packet);
                    fos.write(packet.getData());
                } catch (IOException e) {
                    break;
                }
            }
            fos.close();
            System.out.println("Arquivo recebido! Encerrando transferência.");
        } catch (IOException e1) {
            System.out.println("Erro ao receber arquivo!");
            e1.printStackTrace();
        } finally {
            this.socket.close();
        }        
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

    Map<String, String> myFiles;


    public ClientController(String serverAddress, String nickname) throws IOException {
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.nickname = nickname;
        this.serverPort = 4500;
        this.socket = new DatagramSocket();
        this.myFiles = new ConcurrentHashMap<>();
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

    

    private String createFileHash(File file) throws NoSuchAlgorithmException, IOException {
        // Get file input stream for reading the file content
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        FileInputStream fis = new FileInputStream(file);

        // Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        ;

        fis.close();

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    private void registerFileCommand(String line) {
        String[] tokens = line.split(" ");
        File file = new File(tokens[1]);
        try {
            String fileHash = this.createFileHash(file);
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