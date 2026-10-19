
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPServer {

    public static void main(String[] args) {
        start(new PaymentService());
    }

    public static void start(PaymentService paymentService) {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("UPI TCP Server started on port " + port);

            while (true) {

                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader input = new BufferedReader(
                         new InputStreamReader(
                             clientSocket.getInputStream(),
                             StandardCharsets.UTF_8));
                     PrintWriter output = new PrintWriter(
                         clientSocket.getOutputStream(), true,
                         StandardCharsets.UTF_8)) {

                    clientSocket.setSoTimeout(5000);

                    System.out.println("Client connected.");

                    String receivedRequest = input.readLine();

                    if (receivedRequest == null) {
                        output.println("ERROR|EMPTY_REQUEST");
                        continue;
                    }

                    System.out.println("Received: " + receivedRequest);

                    int lastSeparator = receivedRequest.lastIndexOf('|');

                    if (lastSeparator == -1) {
                        output.println("ERROR|INVALID_REQUEST_FORMAT");
                        continue;
                    }

                    String paymentMessage =
                        receivedRequest.substring(0, lastSeparator);

                    String receivedHmac =
                        receivedRequest.substring(lastSeparator + 1);

                    String calculatedHmac;

                    try {
                        calculatedHmac =
                            HMACUtil.calculateHmac(paymentMessage);
                    } catch (Exception e) {
                        output.println("ERROR|INTEGRITY_CHECK_FAILED");
                        continue;
                    }

                    if (!calculatedHmac.equalsIgnoreCase(receivedHmac)) {
                        output.println("ERROR|INTEGRITY_CHECK_FAILED");
                        System.out.println("HMAC verification failed.");
                        continue;
                    }

                    System.out.println("HMAC VERIFIED");

                    String[] fields = paymentMessage.split("\\|", -1);

                    if (fields.length != 5
                            || !fields[0].equals("PAYMENT")
                            || !fields[1].matches("[A-Za-z0-9-]{1,40}")
                            || !fields[2].matches("\\d{12}")
                            || !fields[3].matches("\\d{12}")) {

                        output.println("ERROR|INVALID_REQUEST_FORMAT");
                        continue;
                    }

                    if (!fields[4].matches("\\d+(\\.\\d{1,2})?")) {
                        output.println("ERROR|INVALID_AMOUNT");
                        continue;
                    }

                    BigDecimal amount;

                    try {
                        amount = new BigDecimal(fields[4]);
                    } catch (NumberFormatException e) {
                        output.println("ERROR|INVALID_AMOUNT");
                        continue;
                    }

                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        output.println("ERROR|INVALID_AMOUNT");
                        continue;
                    }

                    if (fields[2].equals(fields[3])) {
                        output.println("ERROR|SAME_ACCOUNT");
                        continue;
                    }

                    String transactionId = fields[1];

                    String response = paymentService.processPayment(
                        transactionId,
                        fields[2],
                        fields[3],
                        amount
                    );

                    output.println(response);

                    System.out.println("Payment response: " + response);

                    paymentService.displayBalances();
                    paymentService.displayTransactionHistory();

                } catch (Exception e) {
                    System.out.println(
                        "Client/network error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
