package app.dya.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioDTO(
        String address,
        BigDecimal totalUsd,
        BigDecimal netWorthUsd,
        BigDecimal healthFactor,
        List<PositionDTO> positions,
        String lastUpdatedIso
) {
    public record PositionDTO(
            String protocol,      // "Aave", "Compound", "UniswapV3"
            String network,       // "ethereum"
            String asset,         // "DAI", "ETH", "LP-ETH/USDC"
            BigDecimal amount,    // raw units (normalized)
            BigDecimal usdValue,
            BigDecimal apr,       // deposit APR as decimal, e.g., 0.045
            BigDecimal borrowAmount,
            BigDecimal borrowApr,
            String riskStatus     // "OK" | "WARN" | "CRITICAL" (placeholder)
    ) {}
}


