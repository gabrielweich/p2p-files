import java.io.*;

public class Server {
    public static void main(String[] args) throws IOException {
        new ServerThread("4500").start();
    }
}
