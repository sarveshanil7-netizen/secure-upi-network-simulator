import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class UDPServer {

    public static void main(String[] args) {
        start();
    }

    public static void start() {

        try (DatagramSocket serverSocket = new DatagramSocket(5002)) {

            System.out.println("UDP Server started on port 5002.");
            System.out.println("Waiting for payment notifications...");

            while (true) {

                byte[] buffer = new byte[1024];

                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);

                serverSocket.receive(packet);

                String message = new String(
                        packet.getData(),
                        0,
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );

                System.out.println("\nNotification received: " + message);
                System.out.println("Sender IP: " + packet.getAddress());
                System.out.println("Sender port: " + packet.getPort());

                String reply = "ACK|" + message;

                byte[] replyData =
                        reply.getBytes(StandardCharsets.UTF_8);

                DatagramPacket response = new DatagramPacket(
                        replyData,
                        replyData.length,
                        packet.getAddress(),
                        packet.getPort()
                );

                serverSocket.send(response);

                System.out.println("Acknowledgement sent.");
            }

        } catch (Exception e) {
            System.out.println("UDP Server error: " + e.getMessage());
        }
    }
}