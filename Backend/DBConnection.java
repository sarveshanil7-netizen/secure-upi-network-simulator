
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String HOST =
            System.getProperty("db.host", "localhost");

    private static final String PORT =
            System.getProperty("db.port", "3306");

    private static final String DB_NAME =
            System.getProperty("db.name", "secure_upi_db");

    private static final String USER =
            System.getProperty(
                    "db.user",
                    System.getenv().getOrDefault("DB_USER", "root")
            );

    private static final String PASSWORD =
            System.getProperty(
                    "db.password",
                    System.getenv().getOrDefault(
                            "DB_PASSWORD",
                            "SecureUPI@2026"
                    )
            );

    private static final String BASE_URL =
            "jdbc:mysql://" + HOST + ":" + PORT
            + "/?allowPublicKeyRetrieval=true"
            + "&useSSL=false&serverTimezone=UTC";

    private static final String DB_URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?allowPublicKeyRetrieval=true"
            + "&useSSL=false&serverTimezone=UTC";

    private static boolean initialized = false;
    private static boolean dbAvailable = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println(
                    "[DBConnection] MySQL JDBC Driver not found: "
                    + e.getMessage()
            );
        } catch (Exception e) {
            System.err.println(
                    "[DBConnection] Initial connection failed: "
                    + e.getMessage()
            );
        }
    }

    public static synchronized void initDatabase() {
        if (initialized) {
            return;
        }

        try (Connection conn =
                     DriverManager.getConnection(BASE_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS " + DB_NAME
            );

            try (Connection dbConn =
                         DriverManager.getConnection(DB_URL, USER, PASSWORD);
                 Statement dbStmt = dbConn.createStatement()) {

                dbStmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS users ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "username VARCHAR(50) UNIQUE NOT NULL,"
                        + "password VARCHAR(255) NOT NULL,"
                        + "payment_password VARCHAR(255) NOT NULL,"
                        + "account_number VARCHAR(12) UNIQUE NOT NULL,"
                        + "phone VARCHAR(10) UNIQUE NOT NULL,"
                        + "balance DECIMAL(12,2) NOT NULL DEFAULT 5000.00,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ")"
                );

                dbStmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS transactions ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "transaction_id VARCHAR(50) UNIQUE NOT NULL,"
                        + "sender_account VARCHAR(12) NOT NULL,"
                        + "receiver_account VARCHAR(12) NOT NULL,"
                        + "amount DECIMAL(12,2) NOT NULL,"
                        + "status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_sender (sender_account),"
                        + "INDEX idx_receiver (receiver_account)"
                        + ")"
                );

                seedDefaultUsers(dbConn);

                System.out.println(
                        "[DBConnection] Connected to MySQL successfully as '"
                        + USER + "'."
                );

                System.out.println(
                        "[DBConnection] Database '" + DB_NAME
                        + "' and tables verified."
                );

                dbAvailable = true;
                initialized = true;
            }

        } catch (SQLException e) {
            dbAvailable = false;
            initialized = true;

            System.err.println(
                    "[DBConnection] MySQL connection failed: "
                    + e.getMessage()
            );

            System.err.println(
                    "[DBConnection] Check MySQL service, password, "
                    + "database permissions, and JDBC driver."
            );

            System.err.println(
                    "[DBConnection] Database-dependent operations "
                    + "may be unavailable."
            );
        }
    }

    private static void seedDefaultUsers(Connection conn) {

        String[][] defaults = {
            {
                "Sarvesh1", "Computer1@", "Sarvesh@123",
                "111100000001", "9000000001", "5000.00"
            },
            {
                "Srimano2", "Computer1@", "Srimano@123",
                "111100000002", "9000000002", "3500.00"
            },
            {
                "Kasi3", "Computer1@", "Kasi@123",
                "111100000003", "9000000003", "4200.00"
            },
            {
                "Hasiba4", "Computer1@", "Hasiba@123",
                "111100000004", "9000000004", "6000.00"
            }
        };

        String sql =
                "INSERT IGNORE INTO users "
                + "(username, password, payment_password, "
                + "account_number, phone, balance) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String[] user : defaults) {
                ps.setString(1, user[0]);
                ps.setString(2, user[1]);
                ps.setString(3, user[2]);
                ps.setString(4, user[3]);
                ps.setString(5, user[4]);
                ps.setBigDecimal(6, new BigDecimal(user[5]));

                ps.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println(
                    "[DBConnection] Seed error: " + e.getMessage()
            );
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static boolean isAvailable() {
        return dbAvailable;
    }
}
