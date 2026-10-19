
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UDPClient {
    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setSoTimeout(5000);

            InetAddress serverAddress =
                InetAddress.getByName("127.0.0.1");

            String message =
                "UPI_NOTIFICATION|TXN001|PAYMENT_REQUEST_RECEIVED";

            byte[] data = message.getBytes();

            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddress, 5002
            );

            clientSocket.send(packet);
            System.out.println("Notification sent: " + message);

            byte[] buffer = new byte[1024];

            DatagramPacket response =
                new DatagramPacket(buffer, buffer.length);

            clientSocket.receive(response);

            String reply = new String(
                response.getData(), 0, response.getLength()
            );

            System.out.println("Server acknowledgement: " + reply);

        } catch (SocketTimeoutException e) {
            System.out.println("No acknowledgement received within 5 seconds.");
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
