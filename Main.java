import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 3 && args[0].equals("Client"))
            new ClientController(args[1], args[2]).start();
        else if (args.length == 1 && args[0].equals("Server"))
            new ServerThread("4500").start();
        else {
            System.out.println(args.length);
            System.out.println(args[0]);
			System.out.println("Uso: java Main <Client|Server> (Client)<ServerAddress> (Client)<nickname>");
		}
    }    
}