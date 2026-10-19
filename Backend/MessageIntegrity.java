
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MessageIntegrity {

    public static String calculateHash(String message)
            throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hashBytes = digest.digest(
            message.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder hexHash = new StringBuilder();

        for (byte b : hashBytes) {
            hexHash.append(String.format("%02x", b & 0xff));
        }

        return hexHash.toString();
    }

    public static void main(String[] args) throws Exception {

        String originalMessage =
            "PAYMENT|TXN001|111100000001|111100000002|100";

        String originalHash = calculateHash(originalMessage);

        System.out.println("Original message:");
        System.out.println(originalMessage);

        System.out.println("\nSHA-256 hash:");
        System.out.println(originalHash);

        String receivedMessage =
            "PAYMENT|TXN001|111100000001|111100000002|100";

        String receivedHash = calculateHash(receivedMessage);

        System.out.println("\nVerifying original message:");

        if (originalHash.equals(receivedHash)) {
            System.out.println("INTEGRITY VERIFIED");
        } else {
            System.out.println("MESSAGE MODIFIED");
        }

        String modifiedMessage =
            "PAYMENT|TXN001|111100000001|111100000002|900";

        String modifiedHash = calculateHash(modifiedMessage);

        System.out.println("\nVerifying modified message:");

        if (originalHash.equals(modifiedHash)) {
            System.out.println("INTEGRITY VERIFIED");
        } else {
            System.out.println("MESSAGE MODIFIED");
        }
    }
}
