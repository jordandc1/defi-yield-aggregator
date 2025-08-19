package app.dya.service.aave;

import app.dya.api.dto.PortfolioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service fetching positions from the Aave v3 subgraph.
 *
 * The implementation is intentionally lightweight and uses {@link RestTemplate}
 * to query the public subgraph. For unit tests the HTTP layer can be mocked
 * using {@code MockRestServiceServer}.
 */
@Service
public class AaveV3Service {

    private final RestTemplate restTemplate;
    private final String subgraphUrl;

    public AaveV3Service(RestTemplateBuilder restTemplateBuilder,
                         @Value("${aave.v3.subgraph:https://api.thegraph.com/subgraphs/name/aave/protocol-v3}") String subgraphUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.subgraphUrl = subgraphUrl;
    }

    /**
     * Returns a list of Aave positions for the given wallet address.
     */
    public List<PortfolioDTO.PositionDTO> getPositions(String address) {
        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();
        String query = buildQuery(address);
        Map<String, Object> response = executeQuery(query);
        Map<String, Object> user = getUser(response);
        if (user == null) {
            return positions;
        }
        BigDecimal healthFactor = parseWad((String) user.getOrDefault("healthFactor", "0"));
        String riskStatus = riskStatus(healthFactor);

        List<Map<String, Object>> reserves = (List<Map<String, Object>>) user.getOrDefault("reserves", Collections.emptyList());
        for (Map<String, Object> r : reserves) {
            positions.addAll(mapReserve(r, riskStatus));
        }
        return positions;
    }

    private String buildQuery(String address) {
        return """
                { user(id: \"%s\") {\n"
                + "  healthFactor\n"
                + "  reserves: userReserves {\n"
                + "    scaledATokenBalance\n"
                + "    scaledVariableDebt\n"
                + "    reserve { symbol decimals liquidityRate variableBorrowRate price { priceInUsd } }\n"
                + "  }\n"
                + "}}""".formatted(address.toLowerCase());
    }

    private Map<String, Object> executeQuery(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("query", query);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(subgraphUrl, entity, Map.class);
    }

    private Map<String, Object> getUser(Map<String, Object> response) {
        if (response == null) return null;
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) return null;
        return (Map<String, Object>) data.get("user");
    }

    private List<PortfolioDTO.PositionDTO> mapReserve(Map<String, Object> userReserve, String riskStatus) {
        Map<String, Object> reserve = (Map<String, Object>) userReserve.get("reserve");
        String symbol = (String) reserve.get("symbol");
        int decimals = Integer.parseInt(reserve.get("decimals").toString());
        BigDecimal priceUsd = new BigDecimal(((Map<String, Object>) reserve.get("price")).get("priceInUsd").toString());
        BigDecimal liquidityRate = parseRay(reserve.get("liquidityRate").toString());
        BigDecimal variableBorrowRate = parseRay(reserve.get("variableBorrowRate").toString());

        BigDecimal supplied = new BigDecimal(userReserve.getOrDefault("scaledATokenBalance", "0").toString())
                .movePointLeft(decimals);
        BigDecimal borrowed = new BigDecimal(userReserve.getOrDefault("scaledVariableDebt", "0").toString())
                .movePointLeft(decimals);

        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();

        if (supplied.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal suppliedUsd = supplied.multiply(priceUsd);
            positions.add(new PortfolioDTO.PositionDTO(
                    "Aave",
                    "ethereum",
                    symbol,
                    supplied,
                    suppliedUsd,
                    liquidityRate,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    riskStatus,
                    "DEPOSIT"
            ));
        }

        if (borrowed.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal borrowedUsd = borrowed.multiply(priceUsd);
            positions.add(new PortfolioDTO.PositionDTO(
                    "Aave",
                    "ethereum",
                    symbol,
                    borrowed,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    borrowedUsd,
                    variableBorrowRate,
                    riskStatus,
                    "BORROW"
            ));
        }

        return positions;
    }

    private BigDecimal parseRay(String value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value).movePointLeft(27);
    }

    private BigDecimal parseWad(String value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value).movePointLeft(18);
    }

    private String riskStatus(BigDecimal healthFactor) {
        if (healthFactor.compareTo(new BigDecimal("1.1")) < 0) {
            return "CRITICAL";
        } else if (healthFactor.compareTo(new BigDecimal("1.3")) < 0) {
            return "WARN";
        }
        return "OK";
    }
}

