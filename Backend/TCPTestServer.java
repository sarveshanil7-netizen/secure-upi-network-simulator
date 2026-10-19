
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPTestServer {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(5001);

        System.out.println("Test server started on port 5001.");

        Socket client = server.accept();
        System.out.println("Client connected.");

        BufferedReader input = new BufferedReader(
            new InputStreamReader(client.getInputStream())
        );

        PrintWriter output = new PrintWriter(
            client.getOutputStream(), true
        );

        String message = input.readLine();
        System.out.println("Received: " + message);

        System.out.println("Waiting 7 seconds...");
        Thread.sleep(7000);

        output.println("Response from test server");

        client.close();
        server.close();
    }
}
