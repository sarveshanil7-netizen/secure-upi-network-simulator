
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPTimeoutTestClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 5001)) {
            socket.setSoTimeout(5000);

            PrintWriter output = new PrintWriter(
                socket.getOutputStream(), true
            );

            BufferedReader input = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            output.println("TEST_TIMEOUT");

            System.out.println("Waiting for server response...");

            String response = input.readLine();
            System.out.println("Server response: " + response);

        } catch (SocketTimeoutException e) {
            System.out.println(
                "Timeout: Server did not respond within 5 seconds."
            );
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
