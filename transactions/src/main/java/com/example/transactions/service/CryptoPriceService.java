package com.example.transactions.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;

@Service
public class CryptoPriceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();
    
    // Cache
    private final Map<String, Double> priceCache = new HashMap<>();
    private LocalDateTime lastUpdate = LocalDateTime.MIN;
    private static final long CACHE_DURATION_SECONDS = 60;

    public double getPrice(String cryptoSymbol, String fiatSymbol) {
        updatePricesIfNeeded();
        String key = cryptoSymbol.toUpperCase() + "_" + fiatSymbol.toUpperCase();
        return priceCache.getOrDefault(key, getDefaultPrice(cryptoSymbol));
    }
    
    public Map<String, Double> getAllPrices() {
        updatePricesIfNeeded();
        return new HashMap<>(priceCache);
    }

    private synchronized void updatePricesIfNeeded() {
        if (LocalDateTime.now().minusSeconds(CACHE_DURATION_SECONDS).isBefore(lastUpdate) && !priceCache.isEmpty()) {
            return;
        }

        try {
            // Fetch BTC and ETH in USD and EUR
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd,eur";
            String json = restTemplate.getForObject(url, String.class);
            
            // Response format: {"bitcoin":{"usd":96000,"eur":90000},"ethereum":{...}}
            Map<String, Map<String, Double>> response = gson.fromJson(json, new TypeToken<Map<String, Map<String, Double>>>(){}.getType());
            
            if (response != null) {
                // Parse and update cache
                updateCache("BTC", "bitcoin", response);
                updateCache("ETH", "ethereum", response);
                
                lastUpdate = LocalDateTime.now();
                System.out.println("✅ Crypto prices updated from CoinGecko");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to fetch crypto prices: " + e.getMessage());
        }
    }
    
    private void updateCache(String symbol, String id, Map<String, Map<String, Double>> response) {
        if (response.containsKey(id)) {
            Map<String, Double> prices = response.get(id);
            if (prices.containsKey("eur")) {
                priceCache.put(symbol + "_EUR", prices.get("eur"));
            }
            if (prices.containsKey("usd")) {
                priceCache.put(symbol + "_USD", prices.get("usd"));
            }
        }
    }

    private double getDefaultPrice(String symbol) {
         return switch (symbol) {
            case "BTC" -> 40000.0;
            case "ETH" -> 2500.0;
            default -> 1.0;
        };
    }
}
