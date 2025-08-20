package app.dya.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service responsible for interactions with the Aave V3 protocol.
 *
 * <p>This is currently a minimal stub and should be expanded with real
 * integration logic. For now it simply returns a default health factor
 * allowing the rest of the application to be wired and tested.</p>
 */
@Service
public class AaveV3HealthService {

    private final RestTemplate restTemplate;
    private final String subgraphUrl;

    public AaveV3HealthService(RestTemplateBuilder restTemplateBuilder,
                               @Value("${aave.v3.subgraph:https://api.thegraph.com/subgraphs/name/aave/protocol-v3}") String subgraphUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.subgraphUrl = subgraphUrl;
    }

    /**
     * Retrieve the health factor for a wallet. The health factor indicates the
     * safety of a user's borrow position on Aave, where values below 1.0 are
     * subject to liquidation.
     *
     * @param address the wallet address to query
     * @return the health factor as reported by Aave
     */
    public BigDecimal getHealthFactor(String address) {
        String query = buildQuery(address);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("query", query);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(subgraphUrl, entity, Map.class);
            if (response != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    Map<String, Object> user = (Map<String, Object>) data.get("user");
                    if (user != null) {
                        Object hf = user.get("healthFactor");
                        if (hf != null) {
                            return parseWad(hf.toString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return BigDecimal.ONE;
    }

    private String buildQuery(String address) {
        return """
                { user(id: \"%s\") { healthFactor } }
                """.formatted(address.toLowerCase());
    }

    private BigDecimal parseWad(String value) {
        if (value == null) return BigDecimal.ONE;
        try {
            return new BigDecimal(value).movePointLeft(18);
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }
}

