package app.dya.api;

import app.dya.api.dto.*;
import app.dya.service.aave.AaveV3Service;
import app.dya.service.compound.CompoundV2Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/portfolio")
@CrossOrigin(origins = "http://localhost:5173")
public class PortfolioController {

    private final AaveV3Service aaveV3Service;
    private final CompoundV2Service compoundV2Service;

    public PortfolioController(AaveV3Service aaveV3Service, CompoundV2Service compoundV2Service) {
        this.aaveV3Service = aaveV3Service;
        this.compoundV2Service = compoundV2Service;
    }

    @GetMapping("/{address}")
    public PortfolioDTO getPortfolio(@PathVariable String address) {
        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();
        positions.addAll(aaveV3Service.getPositions(address));
        positions.addAll(compoundV2Service.getPositions(address));

        BigDecimal totalUsd = positions.stream()
                .map(PortfolioDTO.PositionDTO::usdValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBorrowUsd = positions.stream()
                .map(PortfolioDTO.PositionDTO::borrowAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netWorthUsd = totalUsd.subtract(totalBorrowUsd);
        BigDecimal healthFactor = totalBorrowUsd.compareTo(BigDecimal.ZERO) > 0
                ? totalUsd.divide(totalBorrowUsd, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new PortfolioDTO(
                address,
                totalUsd,
                netWorthUsd,
                healthFactor,
                positions,
                Instant.now().toString()
        );
    }
}
