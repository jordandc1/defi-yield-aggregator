package app.dya.service.uniswap;

import app.dya.api.dto.PortfolioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service fetching Uniswap v3 positions from the public subgraph.
 *
 * <p>The implementation mirrors {@link app.dya.service.aave.AaveV3Service} and
 * purposely avoids any external dependencies other than Spring's
 * {@link RestTemplate} for ease of testing and simplicity.</p>
 */
@Service
public class UniswapV3Service {

    private final RestTemplate restTemplate;
    private final String subgraphUrl;

    public UniswapV3Service(RestTemplateBuilder restTemplateBuilder,
                            @Value("${uniswap.v3.subgraph:https://api.thegraph.com/subgraphs/name/uniswap/uniswap-v3}") String subgraphUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.subgraphUrl = subgraphUrl;
    }

    /**
     * Fetch Uniswap v3 LP positions for the given wallet address.
     */
    public List<PortfolioDTO.PositionDTO> getPositions(String address) {
        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();
        if (address == null || address.isBlank()) {
            return positions;
        }
        String query = buildQuery(address);
        Map<String, Object> response = executeQuery(query);
        List<Map<String, Object>> rawPositions = extractPositions(response);
        for (Map<String, Object> p : rawPositions) {
            PortfolioDTO.PositionDTO dto = mapPosition(p);
            if (dto != null) {
                positions.add(dto);
            }
        }
        return positions;
    }

    private String buildQuery(String address) {
        return """
                { positions(where: { owner: \"%s\" }) {\n" +
                "  liquidity\n" +
                "  pool {\n" +
                "    liquidity\n" +
                "    sqrtPrice\n" +
                "    tick\n" +
                "    feeTier\n" +
                "    token0 { symbol decimals derivedUSD }\n" +
                "    token1 { symbol decimals derivedUSD }\n" +
                "    totalValueLockedToken0\n" +
                "    totalValueLockedToken1\n" +
                "    totalValueLockedUSD\n" +
                "    volumeUSD\n" +
                "    feesUSD\n" +
                "  }\n" +
                "}}""".formatted(address.toLowerCase());
    }

    private Map<String, Object> executeQuery(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("query", query);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(subgraphUrl, entity, Map.class);
    }

    private List<Map<String, Object>> extractPositions(Map<String, Object> response) {
        if (response == null) return Collections.emptyList();
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) return Collections.emptyList();
        Object positions = data.get("positions");
        if (positions instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    private PortfolioDTO.PositionDTO mapPosition(Map<String, Object> position) {
        BigDecimal liquidity = new BigDecimal(position.getOrDefault("liquidity", "0").toString());
        Map<String, Object> pool = (Map<String, Object>) position.get("pool");
        if (pool == null) return null;
        BigDecimal poolLiquidity = new BigDecimal(pool.getOrDefault("liquidity", "0").toString());
        if (poolLiquidity.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal share = liquidity.divide(poolLiquidity, 18, RoundingMode.HALF_UP);

        BigDecimal reserve0 = new BigDecimal(pool.getOrDefault("totalValueLockedToken0", "0").toString());
        BigDecimal reserve1 = new BigDecimal(pool.getOrDefault("totalValueLockedToken1", "0").toString());

        Map<String, Object> token0 = (Map<String, Object>) pool.get("token0");
        Map<String, Object> token1 = (Map<String, Object>) pool.get("token1");
        String symbol0 = token0.getOrDefault("symbol", "").toString();
        String symbol1 = token1.getOrDefault("symbol", "").toString();
        String feeTier = pool.getOrDefault("feeTier", "").toString();

        BigDecimal token0PriceUsd = new BigDecimal(token0.getOrDefault("derivedUSD", "0").toString());
        BigDecimal token1PriceUsd = new BigDecimal(token1.getOrDefault("derivedUSD", "0").toString());

        BigDecimal token0Amount = reserve0.multiply(share);
        BigDecimal token1Amount = reserve1.multiply(share);

        // usdValue = (token0_amount * token0_priceUSD) + (token1_amount * token1_priceUSD)
        BigDecimal usdValue = token0Amount.multiply(token0PriceUsd)
                .add(token1Amount.multiply(token1PriceUsd));

        BigDecimal tvlUsd = new BigDecimal(pool.getOrDefault("totalValueLockedUSD", "0").toString());
        BigDecimal feesUsd = new BigDecimal(pool.getOrDefault("feesUSD", "0").toString());
        BigDecimal apr = BigDecimal.ZERO;
        if (tvlUsd.compareTo(BigDecimal.ZERO) > 0) {
            apr = feesUsd.divide(tvlUsd, 18, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("365"));
        }

        String asset = "LP-%s/%s-%s".formatted(symbol0, symbol1, feeTier);

        return new PortfolioDTO.PositionDTO(
                "UniswapV3",
                "ethereum",
                asset,
                usdValue,          // treat amount as USD share value
                usdValue,
                apr,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "OK",
                "DEPOSIT"
        );
    }
}

