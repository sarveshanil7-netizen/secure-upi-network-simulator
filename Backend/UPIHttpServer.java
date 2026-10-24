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

    public static void main(String[] args) throws IOException {
        start(new PaymentService());
    }

    public static void start(PaymentService service) throws IOException {
        paymentService = service;

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ========================================================
        // 1. API: Register New User (MySQL Database Integration)
        // ========================================================
        server.createContext("/api/register", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseParameters(body);

                String username = params.get("username");
                String password = params.get("password");
                String paymentPassword = params.get("paymentPassword");
                String phone = params.get("phone");
                String balanceText = params.get("initialBalance");

                BigDecimal initialBalance = new BigDecimal("5000.00");
                if (balanceText != null && balanceText.matches("\\d+(\\.\\d{1,2})?")) {
                    initialBalance = new BigDecimal(balanceText);
                }

                String result = paymentService.registerUser(username, password, paymentPassword, phone, initialBalance);

                if (result.startsWith("SUCCESS|")) {
                    String accountNumber = result.split("\\|")[1];
                    sendResponse(exchange, 201,
                        "{\"success\":true,\"message\":\"User registered successfully!\"," +
                        "\"accountNumber\":\"" + accountNumber + "\"," +
                        "\"username\":\"" + jsonEscape(username) + "\"}");
                } else {
                    String errMsg = result.replace("ERROR|", "").replace("_", " ");
                    sendResponse(exchange, 400,
                        "{\"success\":false,\"message\":\"" + jsonEscape(errMsg) + "\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false,\"message\":\"Registration error: " + jsonEscape(e.getMessage()) + "\"}");
            }
        });

        // ========================================================
        // 2. API: Login & Verify against Database
        // ========================================================
        server.createContext("/api/login", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseParameters(body);

                String username = params.get("username");
                String password = params.get("password");

                PaymentService.User user = paymentService.authenticateUser(username, password);

                if (user != null) {
                    sendResponse(exchange, 200,
                        "{\"success\":true,\"username\":\"" + jsonEscape(user.username) + "\"," +
                        "\"accountNumber\":\"" + user.accountNumber + "\"," +
                        "\"phone\":\"" + user.phone + "\"," +
                        "\"balance\":" + user.balance.toPlainString() + "}");
                } else {
                    sendResponse(exchange, 401,
                        "{\"success\":false,\"message\":\"Invalid credentials. User not registered in database.\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false,\"message\":\"Login error: " + jsonEscape(e.getMessage()) + "\"}");
            }
        });

        // ========================================================
        // 3. API: Global Transactions SQL Table
        // ========================================================
        server.createContext("/api/global-history", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            try {
                List<PaymentService.TransactionRecord> list = paymentService.getGlobalTransactions();
                StringBuilder json = new StringBuilder("[");

                for (int i = 0; i < list.size(); i++) {
                    PaymentService.TransactionRecord tx = list.get(i);
                    if (i > 0) json.append(",");

                    json.append("{")
                        .append("\"transactionId\":\"").append(jsonEscape(tx.transactionId)).append("\",")
                        .append("\"sender\":\"").append(jsonEscape(tx.senderAccount)).append("\",")
                        .append("\"receiver\":\"").append(jsonEscape(tx.receiverAccount)).append("\",")
                        .append("\"amount\":").append(tx.amount.toPlainString()).append(",")
                        .append("\"status\":\"").append(jsonEscape(tx.status)).append("\",")
                        .append("\"timestamp\":\"").append(jsonEscape(tx.timestamp)).append("\"")
                        .append("}");
                }
                json.append("]");

                sendResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
            }
        });

        // ========================================================
        // 4. API: Check Account Balance
        // ========================================================
        server.createContext("/api/balance", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseParameters(exchange.getRequestURI().getRawQuery());
            String account = params.get("account");

            if (account == null || !account.matches("\\d{12}")) {
                sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid account number\"}");
                return;
            }

            BigDecimal balance = paymentService.getBalance(account);

            if (balance == null) {
                sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Account not found\"}");
                return;
            }

            sendResponse(exchange, 200,
                "{\"account\":\"" + account + "\",\"balance\":" + balance.toPlainString() + "}");
        });

        // ========================================================
        // 5. API: Account Look-up by Phone
        // ========================================================
        server.createContext("/api/account-by-phone", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseParameters(exchange.getRequestURI().getRawQuery());
            String phone = params.get("phone");
            String account = paymentService.getAccountByPhone(phone);

            if (account == null) {
                sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Phone number not registered\"}");
                return;
            }

            sendResponse(exchange, 200, "{\"accountNumber\":\"" + account + "\"}");
        });

        // ========================================================
        // 6. API: Legacy In-Memory History
        // ========================================================
        server.createContext("/api/history", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            List<String> history = paymentService.getTransactionHistory();
            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < history.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(jsonEscape(history.get(i))).append("\"");
            }
            json.append("]");

            sendResponse(exchange, 200, json.toString());
        });

        // ========================================================
        // 7. API: Process Payment (Validates Password & Inserts SQL)
        // ========================================================
        server.createContext("/api/payment", exchange -> {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseParameters(body);

                String username = params.get("username");
                String password = params.get("password");
                String receiver = params.get("receiverAccount");
                String amountText = params.get("amount");

                // Validate payment password against MySQL database
                if (username == null || password == null || !paymentService.validatePaymentPassword(username, password)) {
                    sendResponse(exchange, 401, "{\"success\":false,\"message\":\"Invalid payment authorization password\"}");
                    return;
                }

                String sender = paymentService.getAccountByUsername(username);
                if (sender == null) {
                    sendResponse(exchange, 401, "{\"success\":false,\"message\":\"Sender account not found in database.\"}");
                    return;
                }

                if (receiver == null || !receiver.matches("\\d{12}")) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid receiver account number\"}");
                    return;
                }

                if (amountText == null || !amountText.matches("\\d+(\\.\\d{1,2})?")) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Enter a valid amount\"}");
                    return;
                }

                BigDecimal amount = new BigDecimal(amountText);
                String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

                // Process payment (Atomic SQL Debit, Credit & Global Ledger Insert)
                String result = paymentService.processPayment(transactionId, sender, receiver, amount);

                if (result.startsWith("SUCCESS|")) {
                    sendResponse(exchange, 200,
                        "{\"success\":true,\"transactionId\":\"" + transactionId + "\",\"message\":\"Payment completed successfully\"}");
                } else {
                    String error = result.substring(result.indexOf('|') + 1);
                    sendResponse(exchange, 400,
                        "{\"success\":false,\"message\":\"" + jsonEscape(error.replace('_', ' ')) + "\"}");
                }

            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid payment request: " + jsonEscape(e.getMessage()) + "\"}");
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("UPI HTTP Server running at http://localhost:8080");
        System.out.println("Connected with MySQL Database 'secure_upi_db'");
    }

    private static Map<String, String> parseParameters(String input) {
        Map<String, String> params = new HashMap<>();
        if (input == null || input.isEmpty()) return params;

        for (String pair : input.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    private static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
