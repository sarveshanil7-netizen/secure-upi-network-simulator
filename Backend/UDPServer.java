
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer {
    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(5002)) {
            System.out.println("UDP Server started on port 5002.");
            System.out.println("Waiting for notifications...");

            byte[] buffer = new byte[1024];

            DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length);

            serverSocket.receive(packet);

            String message = new String(
                packet.getData(), 0, packet.getLength()
            );

            System.out.println("Notification received: " + message);
            System.out.println("Sender IP: " + packet.getAddress());
            System.out.println("Sender port: " + packet.getPort());

            String reply = "Notification received by UDP server";
            byte[] replyData = reply.getBytes();

            DatagramPacket response = new DatagramPacket(
                replyData,
                replyData.length,
                packet.getAddress(),
                packet.getPort()
            );

            serverSocket.send(response);
            System.out.println("Acknowledgement sent.");

        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
