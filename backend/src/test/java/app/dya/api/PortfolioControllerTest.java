package app.dya.api;

import app.dya.api.dto.PortfolioDTO;
import app.dya.service.aave.AaveV3Service;
import app.dya.service.compound.CompoundV2Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AaveV3Service aaveV3Service;

    @MockBean
    private CompoundV2Service compoundV2Service;

    @Test
    void aggregatesPositionsAndCalculatesTotals() throws Exception {
        List<PortfolioDTO.PositionDTO> aavePositions = List.of(
                new PortfolioDTO.PositionDTO(
                        "Aave", "ethereum", "DAI",
                        new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0.05"),
                        BigDecimal.ZERO, BigDecimal.ZERO, "OK", "DEPOSIT"),
                new PortfolioDTO.PositionDTO(
                        "Aave", "ethereum", "DAI",
                        new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("20"), new BigDecimal("0.03"), "OK", "BORROW")
        );
        List<PortfolioDTO.PositionDTO> compoundPositions = List.of(
                new PortfolioDTO.PositionDTO(
                        "Compound", "ethereum", "USDC",
                        new BigDecimal("50"), new BigDecimal("50"), new BigDecimal("0.02"),
                        BigDecimal.ZERO, BigDecimal.ZERO, "OK", "DEPOSIT")
        );

        when(aaveV3Service.getPositions("0xabc")).thenReturn(aavePositions);
        when(compoundV2Service.getPositions("0xabc")).thenReturn(compoundPositions);

        mockMvc.perform(get("/portfolio/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("0xabc"))
                .andExpect(jsonPath("$.totalUsd").value(150))
                .andExpect(jsonPath("$.netWorthUsd").value(130))
                .andExpect(jsonPath("$.healthFactor").value(7.5))
                .andExpect(jsonPath("$.positions.length()").value(3))
                .andExpect(jsonPath("$.positions[0].positionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.positions[1].positionType").value("BORROW"))
                .andExpect(jsonPath("$.positions[2].asset").value("USDC"));
    }
}

