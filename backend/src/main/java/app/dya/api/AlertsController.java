package app.dya.api;

import app.dya.api.dto.AlertItem;
import app.dya.api.dto.AlertsResponse;
import app.dya.service.AaveV3HealthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "http://localhost:5173")
public class AlertsController {

    private static final BigDecimal RISK_THRESHOLD = new BigDecimal("1.3");

    private final AaveV3HealthService aaveV3Service;

    public AlertsController(AaveV3HealthService aaveV3Service) {
        this.aaveV3Service = aaveV3Service;
    }

    @GetMapping("/{address}")
    public AlertsResponse getAlerts(@PathVariable String address) {
        BigDecimal healthFactor = aaveV3Service.getHealthFactor(address);
        List<AlertItem> alerts = new ArrayList<>();
        Instant now = Instant.now();

        if (healthFactor.compareTo(RISK_THRESHOLD) < 0) {
            alerts.add(new AlertItem(
                    "LIQUIDATION_RISK",
                    String.format("Health factor %.2f below 1.3 on Aave position", healthFactor),
                    "Aave",
                    now.toString()));
        }

        return new AlertsResponse(address, alerts);
    }
}
