package hal.amorce_projet_gd;
import java.util.Collections;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements Serializable {
    private String username;
    private String password;
    private double balance;

    private Map<String, Double> portfolio;
    private Map<String, Portfolio> portfolios;
    public User(String username, String password, double balance) {
        this.username = username;
        this.password = password;
        this.balance = balance;
        this.portfolio = new HashMap<>();
        this.portfolios = new HashMap<>();
    }


    public String getUsername() { return username; }

    public double getBalance() { return balance; }

    public Map<String, Double> getPortfolio() { return portfolio; }

    public void addToPortfolio(String cryptoId, double quantity) { portfolio.put(cryptoId, portfolio.getOrDefault(cryptoId, 0.0) + quantity); }




    public void addPortfolio(String portfolioName, Portfolio portfolio) {
        portfolios.put(portfolioName, portfolio);
    }

    public void removePortfolio(String portfolioName) {
        portfolios.remove(portfolioName);
    }

    public Portfolio getPortfolio(String portfolioName) {
        return portfolios.get(portfolioName);
    }

    public Map<String, Portfolio> getPortfolios() {
        return portfolios;
    }
}
