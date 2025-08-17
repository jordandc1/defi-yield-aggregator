package app.dya.price;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class PriceService {

    private final CoinGeckoClient client;
    private final TimedCache<String, Double> cache;

    public PriceService(
            CoinGeckoClient client,
            @Value("${app.prices.cacheTtlMinutes:10}") long ttlMinutes
    ){
        this.client = client;
        this.cache = new TimedCache<>(Duration.ofMinutes(ttlMinutes <= 0 ? 10 : ttlMinutes));
    }

    /** Returns {"ETH": 2789.12, "DAI": 1.0, "USDC": 1.0} for requested symbols (unsupported ignored). */
    public Map<String, Double> getUsdPrices(List<String> symbols){
        // sanitize + keep only supported
        List<String> req = symbols.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .filter(Symbol::supported)
                .distinct()
                .toList();

        Map<String, Double> out = new LinkedHashMap<>();
        List<Symbol> misses = new ArrayList<>();

        // cache hits
        for(String s : req){
            Double v = cache.get(s);
            if(v != null) out.put(s, v);
            else misses.add(Symbol.from(s));
        }

        // fetch misses
        if(!misses.isEmpty()){
            Set<String> ids = new HashSet<>();
            for(Symbol sym : misses) ids.add(sym.coingeckoId);

            Map<String, Map<String, Double>> resp = client.fetchUsdPricesByIds(ids);

            for(Symbol sym : misses){
                Double px = Optional.ofNullable(resp.get(sym.coingeckoId))
                        .map(m -> m.get("usd"))
                        .orElse(null);
                if(px != null){
                    cache.put(sym.name(), px);
                    out.put(sym.name(), px);
                }
            }
        }
        return out;
    }

    public String ping() {
        return client.ping(); // just forwards to the client
    }
}
