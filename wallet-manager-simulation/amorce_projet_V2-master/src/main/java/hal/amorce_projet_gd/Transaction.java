package hal.amorce_projet_gd;

import java.time.LocalDateTime;

public class Transaction {
    private String username;
    private String type;
    private String crypto;
    private double amount;
    private double pricePerUnit;
    private LocalDateTime dateTime;
    private String portfolioName;



    public String getPortfolioName() {
        return portfolioName;
    }
    public Transaction(String username, String type, String crypto, double amount, double pricePerUnit, LocalDateTime dateTime, String portfolioName) {
        this.username = username;
        this.type = type;
        this.crypto = crypto;
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.dateTime = dateTime;
        this.portfolioName = portfolioName;
    }


    public String getUsername() { return username; }
    public String getType() { return type; }
    public String getCrypto() { return crypto; }
    public double getAmount() { return amount; }
    public double getPricePerUnit() { return pricePerUnit; }
    public LocalDateTime getDateTime() { return dateTime; }
}
