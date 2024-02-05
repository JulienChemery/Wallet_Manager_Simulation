package hal.amorce_projet_gd;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.exception.CoinGeckoApiException;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio {
    private String name;
    private Map<String, Double> assets;
    private CoinGeckoApiClient client;



    public Portfolio(String name, Map<String, Double> assets, CoinGeckoApiClient client) {
        this.name = name;
        this.assets = new HashMap<>(assets);
        this.client = client;
    }
    public String getName() {
        return name;
    }

    public void addToPortfolio(String cryptoId, double quantity) {
        assets.put(cryptoId, assets.getOrDefault(cryptoId, 0.0) + quantity);
    }
    public double getTotalQuantity() {
        double total = 0.0;
        for (double quantity : this.assets.values()) {
            total += quantity;
        }
        return total;
    }





    public void removeQuantity(String cryptoId, double quantity) {
        double currentQty = assets.getOrDefault(cryptoId, 0.0);
        double newQty = Math.max(0, currentQty - quantity);
        if (newQty == 0) {
            assets.remove(cryptoId);
        } else {
            assets.put(cryptoId, newQty);
        }
    }

    public Map<String, Double> getAssets() {
        return assets;
    }

    public List<CryptoAsset> getAssetsList() {
        List<CryptoAsset> assetList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : assets.entrySet()) {
            assetList.add(new CryptoAsset(entry.getKey(), entry.getValue()));
        }
        return assetList;
    }


    public CryptoAsset getAsset(String cryptoId) {
        Double quantity = assets.get(cryptoId);
        if (quantity != null) {
            return new CryptoAsset(cryptoId, quantity);
        } else {
            return null;
        }
    }

}
