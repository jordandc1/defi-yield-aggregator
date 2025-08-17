package app.dya.api;

import app.dya.api.dto.*;
import app.dya.service.aave.AaveV3Service;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/portfolio")
@CrossOrigin(origins = "http://localhost:5173")
public class PortfolioController {

    private final AaveV3Service aaveV3Service;

    public PortfolioController(AaveV3Service aaveV3Service) {
        this.aaveV3Service = aaveV3Service;
    }

    @GetMapping("/{address}")
    public PortfolioDTO getPortfolio(@PathVariable String address) {
        List<PortfolioDTO.PositionDTO> positions = aaveV3Service.getPositions(address);
        BigDecimal totalUsd = positions.stream()
                .map(PortfolioDTO.PositionDTO::usdValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioDTO(
                address,
                totalUsd,
                positions,
                Instant.now().toString()
        );
    }
}
