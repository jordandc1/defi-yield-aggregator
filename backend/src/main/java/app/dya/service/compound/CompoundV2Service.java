package app.dya.service.compound;

import app.dya.api.dto.PortfolioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for fetching positions from the Compound v2 protocol.
 *
 * <p>The implementation is lightweight and focuses on parsing a minimal
 * response structure returned by a supplied API endpoint. It can be expanded
 * with real protocol integration in the future.</p>
 */
@Service
public class CompoundV2Service {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public CompoundV2Service(RestTemplateBuilder restTemplateBuilder,
                             @Value("${compound.v2.api:https://api.compound.finance/v2/account}") String apiUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiUrl = apiUrl;
    }

    /**
     * Retrieve Compound v2 positions for a given wallet address.
     *
     * @param address wallet address
     * @return list of positions held on Compound v2
     */
    public List<PortfolioDTO.PositionDTO> getPositions(String address) {
        String url = String.format("%s/%s", apiUrl, address);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) return Collections.emptyList();

        Map<String, Object> account = (Map<String, Object>) response.get("account");
        if (account == null) return Collections.emptyList();

        List<Map<String, Object>> tokens = (List<Map<String, Object>>) account.getOrDefault("tokens", Collections.emptyList());
        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();
        for (Map<String, Object> token : tokens) {
            positions.addAll(mapToken(token));
        }
        return positions;
    }

    private List<PortfolioDTO.PositionDTO> mapToken(Map<String, Object> token) {
        // CompoundLens returns more verbose field names. Fall back to the
        // simplified names used in earlier iterations for backward
        // compatibility of tests and potential API shims.
        String symbol = token.containsKey("underlyingSymbol")
                ? token.get("underlyingSymbol").toString()
                : token.get("symbol").toString();

        BigDecimal usdPrice = new BigDecimal(
                token.getOrDefault("underlyingPrice",
                        token.getOrDefault("usdPrice", "0")).toString()
        );

        BigDecimal supplyBalance = new BigDecimal(
                token.getOrDefault("balanceUnderlying",
                        token.getOrDefault("supplyBalanceUnderlying",
                                token.getOrDefault("supplyBalance", "0"))).toString()
        );

        BigDecimal borrowBalance = new BigDecimal(
                token.getOrDefault("borrowBalanceUnderlying",
                        token.getOrDefault("borrowBalance", "0")).toString()
        );

        BigDecimal supplyRate = new BigDecimal(
                token.getOrDefault("supplyRatePerBlock",
                        token.getOrDefault("supplyRate", "0")).toString()
        );

        BigDecimal borrowRate = new BigDecimal(
                token.getOrDefault("borrowRatePerBlock",
                        token.getOrDefault("borrowRate", "0")).toString()
        );

        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();

        if (supplyBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usdValue = supplyBalance.multiply(usdPrice);
            positions.add(new PortfolioDTO.PositionDTO(
                    "Compound",
                    "ethereum",
                    symbol,
                    supplyBalance,
                    usdValue,
                    supplyRate,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "OK",
                    "DEPOSIT"
            ));
        }

        if (borrowBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal borrowUsd = borrowBalance.multiply(usdPrice);
            positions.add(new PortfolioDTO.PositionDTO(
                    "Compound",
                    "ethereum",
                    symbol,
                    borrowBalance,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    borrowUsd,
                    borrowRate,
                    "OK",
                    "BORROW"
            ));
        }

        return positions;
    }
}


