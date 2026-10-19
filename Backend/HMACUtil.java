
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HMACUtil {

    private static final String SECRET_KEY =
            "UPI_Demo_Secret_Key_2026";

    public static String calculateHmac(String message)
            throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec key = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(key);

        byte[] hmacBytes = mac.doFinal(
                message.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();

        for (byte b : hmacBytes) {
            result.append(String.format("%02x", b & 0xff));
        }

        return result.toString();
    }
}
