import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UDPClient {

    public static void sendNotification(
            String transactionId, String status) {

        String message =
                "UPI_NOTIFICATION|" + transactionId + "|" + status;

        try (DatagramSocket clientSocket = new DatagramSocket()) {

            clientSocket.setSoTimeout(5000);

            InetAddress serverAddress =
                    InetAddress.getByName("127.0.0.1");

            byte[] data =
                    message.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddress, 5002
            );

            clientSocket.send(packet);

            System.out.println("UDP notification sent: " + message);

            byte[] buffer = new byte[1024];

            DatagramPacket response =
                    new DatagramPacket(buffer, buffer.length);

            clientSocket.receive(response);

            String reply = new String(
                    response.getData(),
                    0,
                    response.getLength(),
                    StandardCharsets.UTF_8
            );

            System.out.println("UDP acknowledgement: " + reply);

        } catch (SocketTimeoutException e) {
            System.out.println(
                    "UDP notification sent, but no acknowledgement arrived."
            );
        } catch (Exception e) {
            System.out.println(
                    "UDP notification error: " + e.getMessage()
            );
        }
    }

    public static void main(String[] args) {
        String transactionId =
                args.length > 0 ? args[0] : "TXN001";

        String status =
                args.length > 1 ? args[1] : "PAYMENT_REQUEST_RECEIVED";

        sendNotification(transactionId, status);
    }
}