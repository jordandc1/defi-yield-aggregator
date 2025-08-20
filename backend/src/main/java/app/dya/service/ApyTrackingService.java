package app.dya.service;

import app.dya.api.dto.AlertItem;
import app.dya.api.dto.PortfolioDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks historical APRs for positions to detect significant yield drops.
 */
@Service
public class ApyTrackingService {

    private final ConcurrentHashMap<String, BigDecimal> lastApr = new ConcurrentHashMap<>();

    /**
     * Check APR for the given position and emit an alert if it dropped more than 20% since
     * the last observation.
     *
     * @param addr wallet address
     * @param pos  portfolio position
     * @return optional alert when APR decreased significantly
     */
    public Optional<AlertItem> checkApy(String addr, PortfolioDTO.PositionDTO pos) {
        BigDecimal currentApr = pos.apr() == null ? BigDecimal.ZERO : pos.apr();
        String key = String.join(":", addr, pos.protocol(), pos.asset(), pos.positionType());
        BigDecimal previous = lastApr.put(key, currentApr);
        if (previous != null && currentApr.compareTo(previous.multiply(new BigDecimal("0.8"))) < 0) {
            String message = String.format("APR dropped from %.2f%% to %.2f%% on %s %s", 
                    previous.multiply(BigDecimal.valueOf(100)),
                    currentApr.multiply(BigDecimal.valueOf(100)),
                    pos.protocol(),
                    pos.asset());
            AlertItem alert = new AlertItem(
                    "YIELD_DROP",
                    message,
                    pos.protocol(),
                    Instant.now().toString()
            );
            return Optional.of(alert);
        }
        return Optional.empty();
    }
}
