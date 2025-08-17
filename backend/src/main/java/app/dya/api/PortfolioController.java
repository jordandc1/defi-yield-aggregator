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
                new BigDecimal("1500"),
                new BigDecimal("1200"),
                new BigDecimal("1.8"),
                List.of(
                        new PortfolioDTO.PositionDTO(
                                "Aave","ethereum","DAI",
                                new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("0.045"),
                                new BigDecimal("300"), new BigDecimal("0.025"),
                                "OK"
                        ),
                        new PortfolioDTO.PositionDTO(
                                "Compound","ethereum","USDC",
                                new BigDecimal("500"), new BigDecimal("500"), new BigDecimal("0.032"),
                                BigDecimal.ZERO, BigDecimal.ZERO,
                                "OK"
                        )
                ),
                Instant.now().toString()
        );
    }
}
