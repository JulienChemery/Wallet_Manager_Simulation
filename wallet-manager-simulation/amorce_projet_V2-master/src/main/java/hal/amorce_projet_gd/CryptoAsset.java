package hal.amorce_projet_gd;

public class CryptoAsset {
    private final String name;
    private final Double quantity;

    public CryptoAsset(String name, Double quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public Double getQuantity() {
        return quantity;
    }
}
