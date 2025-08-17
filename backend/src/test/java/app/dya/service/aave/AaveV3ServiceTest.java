package app.dya.service.aave;

import app.dya.api.dto.PortfolioDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AaveV3ServiceTest {

    @Test
    void parsesPositionsFromSubgraph() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

        AaveV3Service service = new AaveV3Service(new RestTemplateBuilder() {
            @Override
            public RestTemplate build() {
                return restTemplate;
            }
        }, "http://example.com");

        String body = "{" +
                "\"data\":{\"user\":{\"healthFactor\":\"1300000000000000000\"," +
                "\"reserves\":[{\"scaledATokenBalance\":\"100000000000000000000\"," +
                "\"scaledVariableDebt\":\"10000000000000000000\"," +
                "\"reserve\":{\"symbol\":\"DAI\",\"decimals\":\"18\",\"liquidityRate\":\"50000000000000000000000000\",\"variableBorrowRate\":\"100000000000000000000000000\",\"price\":{\"priceInUsd\":\"1\"}}}]}" +
                "}}";

        server.expect(requestTo("http://example.com"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).hasSize(1);
        PortfolioDTO.PositionDTO p = positions.get(0);
        assertThat(p.asset()).isEqualTo("DAI");
        assertThat(p.amount()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(p.apr()).isEqualByComparingTo(new BigDecimal("0.05"));
        assertThat(p.usdValue()).isEqualByComparingTo(new BigDecimal("90"));
        assertThat(p.riskStatus()).isEqualTo("OK");
    }
}

