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
	String name;

	public HeartbeatSender(DatagramSocket socket, InetAddress address, Integer port, String name){
		this.socket = socket;
		this.name = name;
		this.address = address;
		this.port = port;
	}

	public void run(){
		while (!Thread.currentThread().isInterrupted()){
			try {
				byte[] texto = new byte[1024];
				texto = ("heartbeat " + name).getBytes();
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

public class Client {

	public static void sendMessage(DatagramSocket socket, InetAddress address, Integer port, String message) {
		byte[] command = new byte[1024];
		command = message.getBytes();

		try {
			socket.send(new DatagramPacket(command, command.length, address, port));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static InetSocketAddress stringToIp(String string) throws URISyntaxException {
		URI uri = new URI("my://" + string); // may throw URISyntaxException
		String host = uri.getHost();
		int port = uri.getPort();

		if (uri.getHost() == null || uri.getPort() == -1) {
			throw new URISyntaxException(uri.toString(),
					"URI must have host and port parts");
		}

		return new InetSocketAddress (host, port);
	}

	public static void sendToCommand(DatagramSocket socket, String line){
		String[] tokens = line.split(" ");
		if (tokens.length < 3){
			System.out.println("Comando inválido, formato esperado: \"sendto <endereco>:<porta> <mensagem>\"");
			return;
		}
		try {
			InetSocketAddress address = Client.stringToIp(tokens[1]);
			String message = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length));
			Client.sendMessage(socket, InetAddress.getByName(address.getHostName()), address.getPort(), message);
		} catch (UnknownHostException | URISyntaxException e) {
			System.out.println("Unable to send message to " + tokens[1]);
		}

	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Uso: java UpperClient <address> <nickname>");
			return;
		}


		InetAddress address = InetAddress.getByName(args[0]);
		int serverPort = 4500;
		DatagramSocket socket = new DatagramSocket();

		Thread messageListener = new MessageListener(socket);
		Thread heartbeatSender = new HeartbeatSender(socket, address, serverPort, args[1]);

		messageListener.start();
		heartbeatSender.start();


		String loginMessage = "login " + args[1];
		Client.sendMessage(socket, address, serverPort, loginMessage);

		Scanner scanner = new Scanner(System.in);
		while (true) {
			String line = scanner.nextLine();
			if (line.equals("exit")) break;
			else if (line.startsWith("clients")) Client.sendMessage(socket, address, serverPort, "clients");
			else if (line.startsWith("sendto")) Client.sendToCommand(socket, line);
		}


		messageListener.interrupt();
		heartbeatSender.interrupt();
	}
}
