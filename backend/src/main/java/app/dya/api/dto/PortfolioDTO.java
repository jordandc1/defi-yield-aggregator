package app.dya.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioDTO(
        String address,
        BigDecimal totalUsd,
        List<PositionDTO> positions,
        String lastUpdatedIso
) {
    public record PositionDTO(
            String protocol,      // "Aave", "Compound", "UniswapV3"
            String network,       // "ethereum"
            String asset,         // "DAI", "ETH", "LP-ETH/USDC"
            BigDecimal amount,    // raw units (normalized)
            BigDecimal usdValue,
            BigDecimal apr,       // as decimal, e.g., 0.045
            String riskStatus     // "OK" | "WARN" | "CRITICAL" (placeholder)
    ) {}
}


