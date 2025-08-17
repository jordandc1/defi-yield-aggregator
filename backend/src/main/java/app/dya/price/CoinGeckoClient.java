package app.dya.price;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.*;

@Component
public class CoinGeckoClient {

    private static final ParameterizedTypeReference<Map<String, Map<String, Double>>> PRICE_TYPE =
            new ParameterizedTypeReference<>() {}; // NOTE: keep the full generic type here
    private final RestClient rest;
    private final String demoKey;
    private final String proKey;

    public CoinGeckoClient(
            @Value("${app.prices.baseUrl:}") String baseUrlOverride,         // optional override
            @Value("${app.prices.demoApiKey:}") String demoKey,               // demo key
            @Value("${app.prices.proApiKey:}") String proKey,                 // pro key
            @Value("${app.http.connectTimeoutMillis:3000}") int connectTimeoutMillis,
            @Value("${app.http.readTimeoutMillis:4000}") int readTimeoutMillis,
            @Value("${app.http.proxyHost:}") String proxyHost,
            @Value("${app.http.proxyPort:0}") int proxyPort
    ) {
        this.demoKey = demoKey;
        this.proKey = proKey;

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMillis);
        factory.setReadTimeout(readTimeoutMillis);
        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }

        // Choose base URL: Pro if proKey present; otherwise Public.
        String base = (proKey != null && !proKey.isBlank())
                ? "https://pro-api.coingecko.com/api/v3"
                : "https://api.coingecko.com/api/v3";
        if (baseUrlOverride != null && !baseUrlOverride.isBlank()) {
            base = baseUrlOverride;
        }

        var builder = RestClient.builder()
                .baseUrl(base)
                .requestFactory(factory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "DYA-PriceService/1.0");

        // Add header auth (works for both Demo/Pro)
        if (proKey != null && !proKey.isBlank()) {
            builder = builder.defaultHeader("x-cg-pro-api-key", proKey);
        } else if (demoKey != null && !demoKey.isBlank()) {
            builder = builder.defaultHeader("x-cg-demo-api-key", demoKey);
        }

        this.rest = builder.build();
    }

    public Map<String, Map<String, Double>> fetchUsdPricesByIds(Set<String> ids){
        if (ids == null || ids.isEmpty()) return Map.of();
        String idsCsv = String.join(",", new java.util.TreeSet<>(ids));

        // make final copies for lambda capture
        final String keyNameFinal;
        final String keyValFinal;
        if (proKey != null && !proKey.isBlank()) {
            keyNameFinal = "x_cg_pro_api_key";
            keyValFinal  = proKey;
        } else if (demoKey != null && !demoKey.isBlank()) {
            keyNameFinal = "x_cg_demo_api_key";
            keyValFinal  = demoKey;
        } else {
            keyNameFinal = null;
            keyValFinal  = null;
        }

        return rest.get()
                .uri(u -> {
                    var b = u.path("/simple/price")
                            .queryParam("ids", idsCsv)
                            .queryParam("vs_currencies", "usd");
                    if (keyNameFinal != null) {
                        b = b.queryParam(keyNameFinal, keyValFinal);
                    }
                    return b.build();
                })
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PRICE_TYPE);
    }

    // add this method
    public String ping() {
        final String keyNameFinal;
        final String keyValFinal;
        if (proKey != null && !proKey.isBlank()) {
            keyNameFinal = "x_cg_pro_api_key"; keyValFinal = proKey;
        } else if (demoKey != null && !demoKey.isBlank()) {
            keyNameFinal = "x_cg_demo_api_key"; keyValFinal = demoKey;
        } else {
            keyNameFinal = null; keyValFinal = null;
        }

        return rest.get()
                .uri(u -> {
                    var b = u.path("/ping");
                    if (keyNameFinal != null) b = b.queryParam(keyNameFinal, keyValFinal);
                    return b.build();
                })
                .retrieve()
                .body(String.class);
    }
}