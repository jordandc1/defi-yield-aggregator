package app.dya.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsPortfolioWithBorrowFields() throws Exception {
        mockMvc.perform(get("/portfolio/0xabc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthFactor").value(1.8))
                .andExpect(jsonPath("$.netWorthUsd").value(1200))
                .andExpect(jsonPath("$.positions[0].borrowAmount").value(300))
                .andExpect(jsonPath("$.positions[0].borrowApr").value(0.025));
    }
}
