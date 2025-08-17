package app.dya.api;

import app.dya.api.dto.PortfolioDTO;
import app.dya.service.aave.AaveV3Service;
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

    @Test
    void returnsPortfolioFromService() throws Exception {
        List<PortfolioDTO.PositionDTO> positions = List.of(
                new PortfolioDTO.PositionDTO("Aave","ethereum","DAI", new BigDecimal("90"), new BigDecimal("90"), new BigDecimal("0.05"), "OK")
        );
        when(aaveV3Service.getPositions("0xabc")).thenReturn(positions);

        mockMvc.perform(get("/portfolio/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("0xabc"))
                .andExpect(jsonPath("$.totalUsd").value(90))
                .andExpect(jsonPath("$.positions[0].asset").value("DAI"));
    }
}

