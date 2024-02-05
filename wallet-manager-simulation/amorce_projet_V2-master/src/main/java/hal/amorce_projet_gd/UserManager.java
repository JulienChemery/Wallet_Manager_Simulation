package hal.amorce_projet_gd;

import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManager {
    private static final String USER_DATA_FILE = "users.dat";
    private static Map<String, User> users = new HashMap<>();

    static {
        loadUsers();
    }
    public static void updateUserBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void updateUserPortfolio(String username, Map<String, Double> newPortfolio) {
        String deleteSql = "DELETE FROM user_crypto WHERE username = ?";
        String insertSql = "INSERT INTO user_crypto (username, crypto_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            deleteStmt.setString(1, username);
            deleteStmt.executeUpdate();

            for (Map.Entry<String, Double> entry : newPortfolio.entrySet()) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, entry.getKey());
                insertStmt.setDouble(3, entry.getValue());
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password, balance) VALUES(?, ?, 0.0)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(username, password, rs.getDouble("balance"));
                loadUserCrypto(user, conn);
                loadUserPortfolios(user);
                return user;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


    private static void loadUserCrypto(User user, Connection conn) throws SQLException {
        String sql = "SELECT crypto_id, quantity FROM user_crypto WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                user.addToPortfolio(rs.getString("crypto_id"), rs.getDouble("quantity"));
            }
        }
    }


    static void loadUsers() {
        File file = new File(USER_DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    public static User getUser(String username) {
        return users.get(username);
    }






    public static void saveNewPortfolio(String username, Portfolio portfolio) {
        String sql = "INSERT INTO portfolios (username, name) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, portfolio.getName());
            pstmt.executeUpdate();

            // Récupérer l'ID du portefeuille inséré
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int portfolioId = rs.getInt(1);
                savePortfolioAssets(portfolioId, portfolio.getAssets());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void savePortfolioAssets(int portfolioId, Map<String, Double> assets) {
        String sql = "INSERT INTO portfolio_assets (portfolio_id, crypto_id, quantity) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Double> entry : assets.entrySet()) {
                pstmt.setInt(1, portfolioId);
                pstmt.setString(2, entry.getKey());
                pstmt.setDouble(3, entry.getValue());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }



    public static void loadPortfolioData(Portfolio portfolio) {
        String sql = "SELECT crypto_id, quantity FROM portfolio_assets WHERE portfolio_id = (SELECT id FROM portfolios WHERE name = ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, portfolio.getName());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String cryptoId = rs.getString("crypto_id");
                double quantity = rs.getDouble("quantity");
                portfolio.addToPortfolio(cryptoId, quantity);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static void savePortfolioData(String username, Portfolio portfolio) {
        String deleteSql = "DELETE FROM portfolio_assets WHERE portfolio_id = (SELECT id FROM portfolios WHERE name = ? AND username = ?)";
        String insertSql = "INSERT INTO portfolio_assets (portfolio_id, crypto_id, quantity) VALUES ((SELECT id FROM portfolios WHERE name = ? AND username = ?), ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            deleteStmt.setString(1, portfolio.getName());
            deleteStmt.setString(2, username);
            deleteStmt.executeUpdate();

            for (Map.Entry<String,Double> entry : portfolio.getAssets().entrySet()) {
                insertStmt.setString(1, portfolio.getName());
                insertStmt.setString(2, username);
                insertStmt.setString(3, entry.getKey());
                insertStmt.setDouble(4, entry.getValue());
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void loadUserPortfolios(User user) {
        String sql = "SELECT * FROM portfolios WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String portfolioName = rs.getString("name");
                int portfolioId = rs.getInt("id");
                Map<String, Double> assets = loadPortfolioAssets(portfolioId);
                Portfolio portfolio = new Portfolio(portfolioName, assets, new CoinGeckoApiClientImpl());
                user.addPortfolio(portfolioName, portfolio);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Map<String, Double> loadPortfolioAssets(int portfolioId) {
        Map<String, Double> assets = new HashMap<>();
        String sql = "SELECT * FROM portfolio_assets WHERE portfolio_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, portfolioId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String cryptoId = rs.getString("crypto_id");
                double quantity = rs.getDouble("quantity");
                assets.put(cryptoId, quantity);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return assets;
    }











    public static List<Transaction> getTransactions(String username) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getString("username"),
                        rs.getString("type"),
                        rs.getString("crypto"),
                        rs.getDouble("amount"),
                        rs.getDouble("price_per_unit"),
                        LocalDateTime.parse(rs.getString("date_time")),
                        rs.getString("portfolio_name")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return transactions;
    }


    public static void logTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (username, type, crypto, amount, price_per_unit, date_time, portfolio_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection

                     conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {    pstmt.setString(1, transaction.getUsername());
            pstmt.setString(2, transaction.getType());
            pstmt.setString(3, transaction.getCrypto());
            pstmt.setDouble(4, transaction.getAmount());
            pstmt.setDouble(5, transaction.getPricePerUnit());
            pstmt.setString(6, transaction.getDateTime().toString());
            pstmt.setString(7, transaction.getPortfolioName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }}
