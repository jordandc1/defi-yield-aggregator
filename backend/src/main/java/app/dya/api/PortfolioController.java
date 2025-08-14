package app.dya.api;

import app.dya.api.dto.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    @GetMapping("/{address}")
    public PortfolioDTO getPortfolio(@PathVariable String address) {
        // TODO: wire services (Aave/Compound/Uniswap + price service)
        return new PortfolioDTO(
                address,
                new BigDecimal("12345.67"),
                List.of(
                        new PortfolioDTO.PositionDTO("Aave","ethereum","DAI", new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("0.045"), "OK"),
                        new PortfolioDTO.PositionDTO("Compound","ethereum","USDC", new BigDecimal("500"), new BigDecimal("500"), new BigDecimal("0.032"), "OK")
                ),
                Instant.now().toString()
        );
    }
}
