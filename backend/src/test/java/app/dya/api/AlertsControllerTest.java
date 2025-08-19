package app.dya.api;

import app.dya.service.AaveV3HealthService;
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
    private AaveV3HealthService aaveV3Service;

    @Test
    void emitsRiskAlertWhenHealthFactorBelowThreshold() throws Exception {
        when(aaveV3Service.getHealthFactor("0xabc")).thenReturn(new BigDecimal("1.2"));

        mockMvc.perform(get("/alerts/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].type").value("LIQUIDATION_RISK"))
                .andExpect(jsonPath("$.alerts[0].message").value("Health factor 1.20 below 1.3 on Aave position"));
    }

    @Test
    void emitsRiskAlertWhenHealthFactorFarBelowThreshold() throws Exception {
        when(aaveV3Service.getHealthFactor("0xabc")).thenReturn(new BigDecimal("0.9"));

        mockMvc.perform(get("/alerts/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].type").value("LIQUIDATION_RISK"));
    }

    @Test
    void noAlertWhenHealthFactorAboveThreshold() throws Exception {
        when(aaveV3Service.getHealthFactor("0xabc")).thenReturn(new BigDecimal("1.5"));

        mockMvc.perform(get("/alerts/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(0)));
    }
}

