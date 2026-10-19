
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    public static void main(String[] args) {

        String serverAddress = "127.0.0.1";
        int port = 5000;

        try (Socket socket = new Socket(serverAddress, port)) {

            socket.setSoTimeout(5000);

            PrintWriter output = new PrintWriter(
                socket.getOutputStream(), true,
                StandardCharsets.UTF_8);

            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream(),
                    StandardCharsets.UTF_8));

            String paymentMessage ="PAYMENT|TXN102|111100000001|111100000002|100";

            String hmac = HMACUtil.calculateHmac(paymentMessage);
            String request = paymentMessage + "|" + hmac;

            System.out.println("Sending payment request:");
            System.out.println(request);

            output.println(request);

            String response = input.readLine();

            System.out.println("Server response: " + response);

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
