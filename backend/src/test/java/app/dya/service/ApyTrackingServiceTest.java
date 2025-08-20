package app.dya.service;

import app.dya.api.dto.AlertItem;
import app.dya.api.dto.PortfolioDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApyTrackingServiceTest {

    @Test
    void detectsYieldDrop() {
        ApyTrackingService service = new ApyTrackingService();
        PortfolioDTO.PositionDTO initial = new PortfolioDTO.PositionDTO(
                "Aave",
                "ethereum",
                "DAI",
                BigDecimal.ONE,
                BigDecimal.ONE,
                new BigDecimal("0.10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "OK",
                "DEPOSIT"
        );
        PortfolioDTO.PositionDTO lower = new PortfolioDTO.PositionDTO(
                "Aave",
                "ethereum",
                "DAI",
                BigDecimal.ONE,
                BigDecimal.ONE,
                new BigDecimal("0.05"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "OK",
                "DEPOSIT"
        );
        Optional<AlertItem> first = service.checkApy("0xabc", initial);
        assertTrue(first.isEmpty());
        Optional<AlertItem> alert = service.checkApy("0xabc", lower);
        assertTrue(alert.isPresent());
        assertEquals("YIELD_DROP", alert.get().type());
    }
}
