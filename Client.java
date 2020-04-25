import java.io.*;

public class Client {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Uso: java Client <address> <nickname>");
			return;
		}

		new ClientController(args[0], args[1]).start();
	}
}
