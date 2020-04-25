import java.io.*;
import java.net.*;
import java.util.*;




class MessageListener extends Thread{
	DatagramSocket socket;
	public MessageListener(DatagramSocket socket){
		this.socket = socket;
	}

	public void run(){
		while (!Thread.currentThread().isInterrupted()){
			try {
				byte[] texto = new byte[1024];
				// recebe datagrama
				DatagramPacket packet = new DatagramPacket(texto, texto.length);
				socket.setSoTimeout(500);
				socket.receive(packet);
				String message = new String(packet.getData(), 0, packet.getLength());
				System.out.println(packet.getAddress() + ": " + message + '\n');
			} catch (IOException e) {
			}
		}
		System.out.println("Client finished.");
	}
}

class HeartbeatSender extends Thread{
	DatagramSocket socket;
	InetAddress address;
	Integer port;


	public HeartbeatSender(DatagramSocket socket, InetAddress address, Integer port){
		this.socket = socket;
		this.address = address;
		this.port = port;
	}

	public void run(){
		while (!Thread.currentThread().isInterrupted()){
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
		URI uri = new URI("my://" + string); // may throw URISyntaxException
		String host = uri.getHost();
		int port = uri.getPort();

		if (uri.getHost() == null || uri.getPort() == -1) {
			throw new URISyntaxException(uri.toString(),
					"URI must have host and port parts");
		}
		return new InetSocketAddress (host, port);
    }
    
    public void sendToCommand(String line){
		String[] tokens = line.split(" ");
		if (tokens.length < 3){
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

    public ClientController(String serverAddress, String nickname) throws IOException {
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.nickname = nickname;
        this.serverPort = 4500;
        this.socket = new DatagramSocket();
    }

    public void start() {
        
		Thread messageListener = new MessageListener(this.socket);
		Thread heartbeatSender = new HeartbeatSender(this.socket, this.serverAddress, this.serverPort);

		messageListener.start();
		heartbeatSender.start();


		String loginMessage = "login " + this.nickname;
		this.sendMessage(this.serverAddress, serverPort, loginMessage);

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String line = scanner.nextLine();
			if (line.equals("exit")) break;
			else if (line.startsWith("clients")) this.sendMessage(this.serverAddress, serverPort, "clients");
			else if (line.startsWith("sendto")) this.sendToCommand(line);
		}

        scanner.close();
		messageListener.interrupt();
		heartbeatSender.interrupt();
    }
}