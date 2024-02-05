package hal.amorce_projet_gd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:users.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement()) {

            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL, " +
                    "balance REAL NOT NULL)";
            stmt.execute(sqlUsers);

            String sqlUserCrypto = "CREATE TABLE IF NOT EXISTS user_crypto (" +
                    "username TEXT NOT NULL, " +
                    "crypto_id TEXT NOT NULL, " +
                    "quantity REAL NOT NULL, " +
                    "FOREIGN KEY (username) REFERENCES users(username))";
            stmt.execute(sqlUserCrypto);

            String sqlCreateTransactions = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY, " +
                    "username TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "crypto TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "price_per_unit REAL NOT NULL, " +
                    "date_time TEXT NOT NULL, " +
                    "FOREIGN KEY (username) REFERENCES users(username))";
            stmt.execute(sqlCreateTransactions);

            String sqlPortfolios = "CREATE TABLE IF NOT EXISTS portfolios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "FOREIGN KEY (username) REFERENCES users(username))";
            stmt.execute(sqlPortfolios);

            String sqlPortfolioAssets = "CREATE TABLE IF NOT EXISTS portfolio_assets (" +
                    "portfolio_id INTEGER NOT NULL, " +
                    "crypto_id TEXT NOT NULL, " +
                    "quantity REAL NOT NULL, " +
                    "FOREIGN KEY (portfolio_id) REFERENCES portfolios(id))";
            stmt.execute(sqlPortfolioAssets);

            String sqlWallets = "CREATE TABLE IF NOT EXISTS wallets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL, " +
                    "wallet_name TEXT NOT NULL, " +
                    "balance REAL NOT NULL, " +
                    "FOREIGN KEY (username) REFERENCES users(username))";
            stmt.execute(sqlWallets);



        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        String sqlAlterTransactions = "ALTER TABLE transactions ADD COLUMN portfolio_name TEXT";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlAlterTransactions);
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column name")) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void deletePortfolio(String username, String portfolioName) {
        String deletePortfolioSQL = "DELETE FROM portfolios WHERE username = ? AND name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deletePortfolioSQL)) {
            pstmt.setString(1, username);
            pstmt.setString(2, portfolioName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
}
