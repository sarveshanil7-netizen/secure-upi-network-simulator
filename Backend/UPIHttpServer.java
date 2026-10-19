
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UPIHttpServer {

    private static PaymentService paymentService;

    // Unique payment passwords for each user
    private static final Map<String, String> PAYMENT_PASSWORDS =
            new HashMap<>();

    static {
        PAYMENT_PASSWORDS.put("Sarvesh1", "Sarvesh@123");
        PAYMENT_PASSWORDS.put("Srimano2", "Srimano@123");
        PAYMENT_PASSWORDS.put("Kasi3", "Kasi@123");
        PAYMENT_PASSWORDS.put("Hasiba4", "Hasiba@123");
    }

    // Username to account number mapping
    private static final Map<String, String> ACCOUNTS =
            new HashMap<>();

    // Phone number to account number mapping
    private static final Map<String, String> PHONES =
            new HashMap<>();

    static {
        ACCOUNTS.put("Sarvesh1", "111100000001");
        ACCOUNTS.put("Srimano2", "111100000002");
        ACCOUNTS.put("Kasi3", "111100000003");
        ACCOUNTS.put("Hasiba4", "111100000004");

        PHONES.put("9000000001", "111100000001");
        PHONES.put("9000000002", "111100000002");
        PHONES.put("9000000003", "111100000003");
        PHONES.put("9000000004", "111100000004");
    }

    public static void main(String[] args) throws IOException {
        start(new PaymentService());
    }

    public static void start(PaymentService service)
            throws IOException {

        paymentService = service;

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        // API to get account balance
        server.createContext("/api/balance", exchange -> {
            addCorsHeaders(exchange);

            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405,
                        "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseParameters(
                    exchange.getRequestURI().getRawQuery());

            String account = params.get("account");

            if (account == null ||
                    !account.matches("\\d{12}")) {
                sendResponse(exchange, 400,
                        "{\"success\":false,\"message\":\"Invalid account number\"}");
                return;
            }

            BigDecimal balance =
                    paymentService.getBalance(account);

            if (balance == null) {
                sendResponse(exchange, 404,
                        "{\"success\":false,\"message\":\"Account not found\"}");
                return;
            }

            sendResponse(exchange, 200,
                    "{\"account\":\"" + account
                    + "\",\"balance\":"
                    + balance.toPlainString() + "}");
        });

        // API to get transaction history
        server.createContext("/api/history", exchange -> {
            addCorsHeaders(exchange);

            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405,
                        "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            List<String> history =
                    paymentService.getTransactionHistory();

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < history.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }

                json.append("\"")
                    .append(jsonEscape(history.get(i)))
                    .append("\"");
            }

            json.append("]");

            sendResponse(exchange, 200, json.toString());
        });

        // API to process payments
        server.createContext("/api/payment", exchange -> {
            addCorsHeaders(exchange);

            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405,
                        "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);

                Map<String, String> params =
                        parseParameters(body);

                String username = params.get("username");
                String password = params.get("password");
                String receiver = params.get("receiverAccount");
                String amountText = params.get("amount");

                // Validate the unique payment password
                if (username == null || password == null
                        || !PAYMENT_PASSWORDS.containsKey(username)
                        || !PAYMENT_PASSWORDS.get(username)
                                .equals(password)) {

                    sendResponse(exchange, 401,
                            "{\"success\":false,\"message\":\"Invalid payment password\"}");
                    return;
                }

                // Get the sender's account number
                String sender = ACCOUNTS.get(username);

                if (sender == null) {
                    sendResponse(exchange, 401,
                            "{\"success\":false,\"message\":\"Unknown user. Please log in again.\"}");
                    return;
                }

                // Validate receiver account number
                if (receiver == null
                        || !receiver.matches("\\d{12}")) {
                    sendResponse(exchange, 400,
                            "{\"success\":false,\"message\":\"Invalid receiver account number\"}");
                    return;
                }

                // Validate payment amount
                if (amountText == null
                        || !amountText.matches("\\d+(\\.\\d{1,2})?")) {
                    sendResponse(exchange, 400,
                            "{\"success\":false,\"message\":\"Enter a valid amount\"}");
                    return;
                }

                BigDecimal amount = new BigDecimal(amountText);

                // Generate a unique transaction ID
                String transactionId = "TXN-"
                        + UUID.randomUUID()
                                .toString().replace("-", "");

                // Process payment through PaymentService
                String result = paymentService.processPayment(
                        transactionId, sender, receiver, amount);

                if (result.startsWith("SUCCESS|")) {
                    sendResponse(exchange, 200,
                            "{\"success\":true,\"transactionId\":\""
                            + transactionId
                            + "\",\"message\":\"Payment completed successfully\"}");
                } else {
                    String error = result.substring(
                            result.indexOf('|') + 1);

                    sendResponse(exchange, 400,
                            "{\"success\":false,\"message\":\""
                            + jsonEscape(error.replace('_', ' '))
                            + "\"}");
                }

            } catch (Exception e) {
                sendResponse(exchange, 400,
                        "{\"success\":false,\"message\":\"Invalid payment request\"}");
            }
        });

        // API to find account number using phone number
        server.createContext("/api/account-by-phone", exchange -> {
            addCorsHeaders(exchange);

            if (handleOptions(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod()
                    .equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405,
                        "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseParameters(
                    exchange.getRequestURI().getRawQuery());

            String phone = params.get("phone");
            String account = phone == null
                    ? null : PHONES.get(phone);

            if (account == null) {
                sendResponse(exchange, 404,
                        "{\"success\":false,\"message\":\"Phone number not registered\"}");
                return;
            }

            sendResponse(exchange, 200,
                    "{\"accountNumber\":\"" + account + "\"}");
        });

        server.setExecutor(null);
        server.start();

        System.out.println(
                "UPI HTTP API started at http://localhost:8080");
    }

    // Convert URL parameters into a map
    private static Map<String, String> parseParameters(
            String input) {

        Map<String, String> params = new HashMap<>();

        if (input == null || input.isEmpty()) {
            return params;
        }

        for (String pair : input.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                String key = URLDecoder.decode(
                        parts[0], StandardCharsets.UTF_8);

                String value = URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8);

                params.put(key, value);
            }
        }

        return params;
    }

    // Handle browser CORS preflight requests
    private static boolean handleOptions(
            HttpExchange exchange) throws IOException {

        if (exchange.getRequestMethod()
                .equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }

        return false;
    }

    // Add CORS and JSON response headers
    private static void addCorsHeaders(
            HttpExchange exchange) {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, OPTIONS");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
    }

    // Escape special characters in JSON strings
    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    // Send an HTTP response
    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String response) throws IOException {

        byte[] bytes = response.getBytes(
                StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output =
                     exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
