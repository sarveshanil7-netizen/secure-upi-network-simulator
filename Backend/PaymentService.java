
import java.math.BigDecimal;
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

    public PaymentService() {
        balances.put("111100000001", new BigDecimal("5000.00"));
        balances.put("111100000002", new BigDecimal("3500.00"));
        balances.put("111100000003", new BigDecimal("4200.00"));
        balances.put("111100000004", new BigDecimal("6000.00"));
    }

    public synchronized String processPayment(
            String transactionId,
            String sender,
            String receiver,
            BigDecimal amount) {

        if (processedTransactions.contains(transactionId)) {
            return "ERROR|DUPLICATE_TRANSACTION";
        }

        if (!balances.containsKey(sender)
                || !balances.containsKey(receiver)) {
            return "ERROR|ACCOUNT_NOT_FOUND";
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "ERROR|INVALID_AMOUNT";
        }

        if (sender.equals(receiver)) {
            return "ERROR|SAME_ACCOUNT";
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

    public BigDecimal getBalance(String accountNumber) {
        return balances.get(accountNumber);
    }

    public List<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    public synchronized void displayBalances() {
        System.out.println("\n--- ACCOUNT BALANCES ---");

        for (Map.Entry<String, BigDecimal> entry
                : balances.entrySet()) {
            System.out.println(
                    entry.getKey() + " : Rs." + entry.getValue());
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
