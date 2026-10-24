import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaymentService {

    private final Map<String, BigDecimal> balances = new HashMap<>();
    private final Set<String> processedTransactions = new HashSet<>();
    private final List<String> transactionHistory = new ArrayList<>();

    // User Model
    public static class User {
        public String username;
        public String accountNumber;
        public String phone;
        public BigDecimal balance;

        public User(String username, String accountNumber, String phone, BigDecimal balance) {
            this.username = username;
            this.accountNumber = accountNumber;
            this.phone = phone;
            this.balance = balance;
        }
    }

    // Global Transaction Model
    public static class TransactionRecord {
        public String transactionId;
        public String senderAccount;
        public String receiverAccount;
        public BigDecimal amount;
        public String status;
        public String timestamp;

        public TransactionRecord(String transactionId, String senderAccount, String receiverAccount,
                                 BigDecimal amount, String status, String timestamp) {
            this.transactionId = transactionId;
            this.senderAccount = senderAccount;
            this.receiverAccount = receiverAccount;
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    public PaymentService() {
        // Fallback default in-memory balances
        balances.put("111100000001", new BigDecimal("5000.00"));
        balances.put("111100000002", new BigDecimal("3500.00"));
        balances.put("111100000003", new BigDecimal("4200.00"));
        balances.put("111100000004", new BigDecimal("6000.00"));

        syncFromDatabase();
    }

    /**
     * Synchronize in-memory balances from MySQL users table.
     */
    public synchronized void syncFromDatabase() {
        if (!DBConnection.isAvailable()) return;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT account_number, balance FROM users")) {

            while (rs.next()) {
                balances.put(rs.getString("account_number"), rs.getBigDecimal("balance"));
            }
        } catch (SQLException e) {
            System.err.println("[PaymentService] DB Sync warning: " + e.getMessage());
        }
    }

    /**
     * Requirement 1: Register a new user in the MySQL Database.
     */
    public synchronized String registerUser(String username, String password, String paymentPassword,
                                           String phone, BigDecimal initialBalance) {
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            paymentPassword == null || paymentPassword.trim().isEmpty() ||
            phone == null || !phone.matches("\\d{10}")) {
            return "ERROR|INVALID_FIELDS";
        }

        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            initialBalance = new BigDecimal("5000.00");
        }

        String assignedAccountNumber = generateNextAccountNumber();

        if (DBConnection.isAvailable()) {
            try (Connection conn = DBConnection.getConnection()) {
                // Check if username or phone already exists
                String checkSql = "SELECT username, phone FROM users WHERE username = ? OR phone = ?";
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setString(1, username);
                    checkPs.setString(2, phone);
                    ResultSet rs = checkPs.executeQuery();
                    if (rs.next()) {
                        if (username.equalsIgnoreCase(rs.getString("username"))) {
                            return "ERROR|USERNAME_TAKEN";
                        }
                        return "ERROR|PHONE_ALREADY_REGISTERED";
                    }
                }

                // Insert new registered user
                String insertSql = "INSERT INTO users (username, password, payment_password, account_number, phone, balance) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setString(1, username);
                    insertPs.setString(2, password);
                    insertPs.setString(3, paymentPassword);
                    insertPs.setString(4, assignedAccountNumber);
                    insertPs.setString(5, phone);
                    insertPs.setBigDecimal(6, initialBalance);
                    insertPs.executeUpdate();
                }

                balances.put(assignedAccountNumber, initialBalance);
                return "SUCCESS|" + assignedAccountNumber;
            } catch (SQLException e) {
                System.err.println("[PaymentService] Register SQL error: " + e.getMessage());
                return "ERROR|DB_ERROR: " + e.getMessage();
            }
        } else {
            // In-memory fallback
            balances.put(assignedAccountNumber, initialBalance);
            return "SUCCESS|" + assignedAccountNumber;
        }
    }

    /**
     * Requirement 2: Authenticate user against MySQL Database.
     */
    public User authenticateUser(String username, String password) {
        if (username == null || password == null) return null;

        if (DBConnection.isAvailable()) {
            String sql = "SELECT username, account_number, phone, balance FROM users WHERE username = ? AND password = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return new User(
                        rs.getString("username"),
                        rs.getString("account_number"),
                        rs.getString("phone"),
                        rs.getBigDecimal("balance")
                    );
                }
            } catch (SQLException e) {
                System.err.println("[PaymentService] Auth SQL error: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Validate payment password for a given user from the MySQL Database.
     */
    public boolean validatePaymentPassword(String username, String paymentPassword) {
        if (username == null || paymentPassword == null) return false;

        if (DBConnection.isAvailable()) {
            String sql = "SELECT id FROM users WHERE username = ? AND payment_password = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, paymentPassword);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                System.err.println("[PaymentService] Payment password check error: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Look up user's account number by username.
     */
    public String getAccountByUsername(String username) {
        if (username == null) return null;

        if (DBConnection.isAvailable()) {
            String sql = "SELECT account_number FROM users WHERE username = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("account_number");
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }

    /**
     * Look up user's account number by phone from MySQL Database.
     */
    public String getAccountByPhone(String phone) {
        if (phone == null) return null;

        if (DBConnection.isAvailable()) {
            String sql = "SELECT account_number FROM users WHERE phone = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, phone);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("account_number");
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }

    /**
     * Process payment with atomic SQL transactions & record in the global transactions table.
     */
    public synchronized String processPayment(
            String transactionId,
            String sender,
            String receiver,
            BigDecimal amount) {

        if (processedTransactions.contains(transactionId)) {
            return "ERROR|DUPLICATE_TRANSACTION";
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "ERROR|INVALID_AMOUNT";
        }

        if (sender.equals(receiver)) {
            return "ERROR|SAME_ACCOUNT";
        }

        if (DBConnection.isAvailable()) {
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false); // Begin SQL Transaction

                try {
                    // 1. Check duplicate transaction
                    String dupCheck = "SELECT id FROM transactions WHERE transaction_id = ?";
                    try (PreparedStatement dupPs = conn.prepareStatement(dupCheck)) {
                        dupPs.setString(1, transactionId);
                        ResultSet rsDup = dupPs.executeQuery();
                        if (rsDup.next()) {
                            conn.rollback();
                            return "ERROR|DUPLICATE_TRANSACTION";
                        }
                    }

                    // 2. Fetch sender balance with row lock
                    BigDecimal senderBalance = null;
                    String senderLock = "SELECT balance FROM users WHERE account_number = ? FOR UPDATE";
                    try (PreparedStatement psSender = conn.prepareStatement(senderLock)) {
                        psSender.setString(1, sender);
                        ResultSet rsSender = psSender.executeQuery();
                        if (rsSender.next()) {
                            senderBalance = rsSender.getBigDecimal("balance");
                        } else {
                            conn.rollback();
                            return "ERROR|ACCOUNT_NOT_FOUND";
                        }
                    }

                    // 3. Check receiver exists
                    String receiverCheck = "SELECT id FROM users WHERE account_number = ? FOR UPDATE";
                    try (PreparedStatement psRecv = conn.prepareStatement(receiverCheck)) {
                        psRecv.setString(1, receiver);
                        ResultSet rsRecv = psRecv.executeQuery();
                        if (!rsRecv.next()) {
                            conn.rollback();
                            return "ERROR|ACCOUNT_NOT_FOUND";
                        }
                    }

                    // 4. Check sufficient balance
                    if (senderBalance.compareTo(amount) < 0) {
                        conn.rollback();
                        return "ERROR|INSUFFICIENT_BALANCE";
                    }

                    // 5. Debit sender
                    String debitSql = "UPDATE users SET balance = balance - ? WHERE account_number = ?";
                    try (PreparedStatement psDebit = conn.prepareStatement(debitSql)) {
                        psDebit.setBigDecimal(1, amount);
                        psDebit.setString(2, sender);
                        psDebit.executeUpdate();
                    }

                    // 6. Credit receiver
                    String creditSql = "UPDATE users SET balance = balance + ? WHERE account_number = ?";
                    try (PreparedStatement psCredit = conn.prepareStatement(creditSql)) {
                        psCredit.setBigDecimal(1, amount);
                        psCredit.setString(2, receiver);
                        psCredit.executeUpdate();
                    }

                    // 7. Requirement 3: Insert into SQL 'transactions' table (Global history)
                    String insertTxn = "INSERT INTO transactions (transaction_id, sender_account, receiver_account, amount, status) VALUES (?, ?, ?, ?, 'SUCCESS')";
                    try (PreparedStatement psTxn = conn.prepareStatement(insertTxn)) {
                        psTxn.setString(1, transactionId);
                        psTxn.setString(2, sender);
                        psTxn.setString(3, receiver);
                        psTxn.setBigDecimal(4, amount);
                        psTxn.executeUpdate();
                    }

                    conn.commit(); // Transaction Successful!

                    // Sync in-memory balances
                    balances.put(sender, senderBalance.subtract(amount));
                    balances.put(receiver, balances.getOrDefault(receiver, BigDecimal.ZERO).add(amount));
                    processedTransactions.add(transactionId);

                    String record = transactionId
                            + " | From: " + sender
                            + " | To: " + receiver
                            + " | Amount: Rs." + amount
                            + " | SUCCESS";
                    transactionHistory.add(record);

                    return "SUCCESS|" + transactionId + "|PAYMENT_COMPLETED";

                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            } catch (Exception e) {
                System.err.println("[PaymentService] Transaction failed: " + e.getMessage());
                return "ERROR|TRANSACTION_FAILED: " + e.getMessage();
            }
        }

        // Fallback for in-memory mode if DB is not available
        if (!balances.containsKey(sender) || !balances.containsKey(receiver)) {
            return "ERROR|ACCOUNT_NOT_FOUND";
        }
        if (balances.get(sender).compareTo(amount) < 0) {
            return "ERROR|INSUFFICIENT_BALANCE";
        }

        BigDecimal senderBalance = balances.get(sender);
        BigDecimal receiverBalance = balances.get(receiver);
        balances.put(sender, senderBalance.subtract(amount));
        balances.put(receiver, receiverBalance.add(amount));
        processedTransactions.add(transactionId);

        String record = transactionId
                + " | From: " + sender
                + " | To: " + receiver
                + " | Amount: Rs." + amount
                + " | SUCCESS";
        transactionHistory.add(record);

        return "SUCCESS|" + transactionId + "|PAYMENT_COMPLETED";
    }

    /**
     * Requirement 3: Retrieve all transactions from the SQL 'transactions' table.
     */
    public List<TransactionRecord> getGlobalTransactions() {
        List<TransactionRecord> list = new ArrayList<>();

        if (DBConnection.isAvailable()) {
            String sql = "SELECT transaction_id, sender_account, receiver_account, amount, status, " +
                         "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS formatted_date " +
                         "FROM transactions ORDER BY id DESC";

            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    list.add(new TransactionRecord(
                        rs.getString("transaction_id"),
                        rs.getString("sender_account"),
                        rs.getString("receiver_account"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status"),
                        rs.getString("formatted_date")
                    ));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[PaymentService] Global transactions query error: " + e.getMessage());
            }
        }

        // Fallback to in-memory records
        for (String record : transactionHistory) {
            String[] parts = record.split("\\|");
            if (parts.length >= 5) {
                String tid = parts[0].trim();
                String sender = parts[1].replace("From:", "").trim();
                String receiver = parts[2].replace("To:", "").trim();
                String amt = parts[3].replace("Amount:", "").replace("Rs.", "").trim();
                String status = parts[4].trim();
                list.add(new TransactionRecord(tid, sender, receiver, new BigDecimal(amt), status, "Recently"));
            }
        }

        return list;
    }

    public BigDecimal getBalance(String accountNumber) {
        if (DBConnection.isAvailable()) {
            String sql = "SELECT balance FROM users WHERE account_number = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            } catch (SQLException ignored) {}
        }
        return balances.get(accountNumber);
    }

    public List<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    private String generateNextAccountNumber() {
        if (DBConnection.isAvailable()) {
            String sql = "SELECT MAX(account_number) AS max_acc FROM users WHERE account_number LIKE '1111%'";
            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                if (rs.next() && rs.getString("max_acc") != null) {
                    long current = Long.parseLong(rs.getString("max_acc"));
                    return String.valueOf(current + 1);
                }
            } catch (Exception ignored) {}
        }
        long next = 111100000001L + balances.size();
        return String.valueOf(next);
    }

    public synchronized void displayBalances() {
        System.out.println("\n--- ACCOUNT BALANCES ---");
        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            System.out.println(entry.getKey() + " : Rs." + entry.getValue());
        }
    }

    public void displayTransactionHistory() {
        System.out.println("\n--- TRANSACTION HISTORY ---");
        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        for (String transaction : getTransactionHistory()) {
            System.out.println(transaction);
        }
    }
}
