package app.dya.api;

import app.dya.api.dto.AlertItem;
import app.dya.api.dto.AlertsResponse;
import app.dya.api.dto.PortfolioDTO;
import app.dya.api.dto.SubscribeRequest;
import app.dya.service.AaveV3HealthService;
import app.dya.service.ApyTrackingService;
import app.dya.service.AlertSubscriptionService;
import app.dya.service.EmailAlertService;
import app.dya.service.aave.AaveV3Service;
import app.dya.service.compound.CompoundV2Service;
import app.dya.service.uniswap.UniswapV3Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "*")
public class AlertsController {

    private static final BigDecimal RISK_THRESHOLD = new BigDecimal("1.3");

    private final AaveV3HealthService aaveV3HealthService;
    private final AaveV3Service aaveV3Service;
    private final CompoundV2Service compoundV2Service;
    private final UniswapV3Service uniswapV3Service;
    private final ApyTrackingService apyTrackingService;
    private final AlertSubscriptionService subscriptionService;
    private final EmailAlertService emailAlertService;

    public AlertsController(AaveV3HealthService aaveV3HealthService,
                            AaveV3Service aaveV3Service,
                            CompoundV2Service compoundV2Service,
                            UniswapV3Service uniswapV3Service,
                            ApyTrackingService apyTrackingService,
                            AlertSubscriptionService subscriptionService,
                            EmailAlertService emailAlertService) {
        this.aaveV3HealthService = aaveV3HealthService;
        this.aaveV3Service = aaveV3Service;
        this.compoundV2Service = compoundV2Service;
        this.uniswapV3Service = uniswapV3Service;
        this.apyTrackingService = apyTrackingService;
        this.subscriptionService = subscriptionService;
        this.emailAlertService = emailAlertService;
    }

    @GetMapping("/{address}")
    public AlertsResponse getAlerts(@PathVariable String address) {
        BigDecimal healthFactor = aaveV3HealthService.getHealthFactor(address);
        List<AlertItem> alerts = new ArrayList<>();
        Instant now = Instant.now();

        if (healthFactor.compareTo(RISK_THRESHOLD) < 0) {
            alerts.add(new AlertItem(
                    "LIQUIDATION_RISK",
                    String.format("Health factor %.2f below 1.3 on Aave position", healthFactor),
                    "Aave",
                    now.toString()));
        }

        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();
        positions.addAll(aaveV3Service.getPositions(address));
        positions.addAll(compoundV2Service.getPositions(address));
        positions.addAll(uniswapV3Service.getPositions(address));

        for (PortfolioDTO.PositionDTO pos : positions) {
            if ("DEPOSIT".equalsIgnoreCase(pos.positionType())) {
                apyTrackingService.checkApy(address, pos).ifPresent(alerts::add);
            }
        }

        AlertsResponse response = new AlertsResponse(address, alerts);
        if (!alerts.isEmpty()) {
            subscriptionService.getEmail(address)
                    .ifPresent(email -> emailAlertService.send(email, alerts));
        }
        return response;
    }

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody SubscribeRequest request) {
        subscriptionService.subscribeEmail(request.address(), request.email());
    }
}
