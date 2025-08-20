package app.dya.api;

import app.dya.service.AaveV3HealthService;
import app.dya.service.ApyTrackingService;
import app.dya.service.aave.AaveV3Service;
import app.dya.service.compound.CompoundV2Service;
import app.dya.service.uniswap.UniswapV3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertsController.class)
class AlertsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AaveV3HealthService aaveV3HealthService;
    @MockBean
    private AaveV3Service aaveV3Service;
    @MockBean
    private CompoundV2Service compoundV2Service;
    @MockBean
    private UniswapV3Service uniswapV3Service;
    @MockBean
    private ApyTrackingService apyTrackingService;

    @Test
    void emitsRiskAlertWhenHealthFactorBelowThreshold() throws Exception {
        when(aaveV3HealthService.getHealthFactor("0xabc")).thenReturn(new BigDecimal("1.2"));
        when(aaveV3Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());
        when(compoundV2Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());
        when(uniswapV3Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/alerts/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].type").value("LIQUIDATION_RISK"))
                .andExpect(jsonPath("$.alerts[0].message").value("Health factor 1.20 below 1.3 on Aave position"));
    }

    @Test
    void emitsRiskAlertWhenHealthFactorFarBelowThreshold() throws Exception {
        when(aaveV3HealthService.getHealthFactor("0xabc")).thenReturn(new BigDecimal("0.9"));
        when(aaveV3Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());
        when(compoundV2Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());
        when(uniswapV3Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/alerts/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].type").value("LIQUIDATION_RISK"));
    }

    @Test
    void noAlertWhenHealthFactorAboveThreshold() throws Exception {
        when(aaveV3HealthService.getHealthFactor("0xabc")).thenReturn(new BigDecimal("1.5"));
        when(aaveV3Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());
        when(compoundV2Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());
        when(uniswapV3Service.getPositions("0xabc")).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/alerts/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(0)));
    }
}

