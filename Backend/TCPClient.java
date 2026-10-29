import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static final int TIMEOUT_MILLIS = 5000;

    public static String sendPayment(
            String transactionId,
            String senderAccount,
            String receiverAccount,
            BigDecimal amount) throws Exception {

        if (transactionId == null || senderAccount == null
                || receiverAccount == null || amount == null) {
            throw new IllegalArgumentException("Payment fields cannot be null.");
        }

        // The HMAC is calculated over exactly the first five fields.
        String paymentMessage = "PAYMENT|"
                + transactionId + "|"
                + senderAccount + "|"
                + receiverAccount + "|"
                + amount.toPlainString();

        String hmac = HMACUtil.calculateHmac(paymentMessage);

        System.out.println("Client-calculated HMAC: " + hmac);

        // Use the correct HMAC for normal transactions
        String hmacToSend = hmac;

        // Uncomment this line only to demonstrate HMAC failure
        //hmacToSend = "invalid_hmac_for_testing";

        String request = paymentMessage + "|" + hmacToSend;

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            socket.setSoTimeout(TIMEOUT_MILLIS);

            try (
                PrintWriter output = new PrintWriter(
                        socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(), StandardCharsets.UTF_8))
            ) {
                System.out.println("Sending payment request through TCP:");
                System.out.println(paymentMessage);

                output.println(request);

                String response = input.readLine();

                if (response == null) {
                    throw new IOException("TCP server closed without sending a response.");
                }

                if (response != null && !response.startsWith("SUCCESS|")) {
                    System.out.println("TCP Server response: " + response);
                }
                
                return response;
            }
        }
    }
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(
                    "Usage: java TCPClient <transactionId> "
                    + "<senderAccount> <receiverAccount> <amount>");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(args[3]);
            String response = sendPayment(args[0], args[1], args[2], amount);
            System.out.println("Final response: " + response);
        } catch (Exception e) {
            System.out.println("TCP payment request failed: " + e.getMessage());
        }
    }
}
