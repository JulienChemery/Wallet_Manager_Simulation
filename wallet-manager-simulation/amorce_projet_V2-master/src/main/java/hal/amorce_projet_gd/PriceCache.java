package hal.amorce_projet_gd;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.exception.CoinGeckoApiException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PriceCache {
    private final ConcurrentHashMap<String, Double> priceMap = new ConcurrentHashMap<>();
    private final CoinGeckoApiClient client;
    private final long cacheDurationMs;


    public PriceCache(CoinGeckoApiClient client, long cacheDurationMs) {
        this.client = client;
        this.cacheDurationMs = cacheDurationMs;
    }


}
