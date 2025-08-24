package app.dya.service.uniswap;

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
import org.springframework.http.HttpMethod;

class UniswapV3ServiceTest {

    private UniswapV3Service buildService(RestTemplate restTemplate) {
        return new UniswapV3Service(new RestTemplateBuilder() {
            @Override
            public RestTemplate build() {
                return restTemplate;
            }
        }, "http://example.com");
    }

    @Test
    void mapsPositionFromSubgraph() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        UniswapV3Service service = buildService(restTemplate);

        String body = """
            {
              "data": {
                "positions": [
                  {
                    "liquidity": "1000",
                    "pool": {
                      "liquidity": "10000",
                      "totalValueLockedToken0": "100",
                      "totalValueLockedToken1": "200",
                      "token0": { "symbol": "ETH", "derivedUSD": "2" },
                      "token1": { "symbol": "USDC", "derivedUSD": "1" },
                      "feeTier": "500",
                      "totalValueLockedUSD": "50000",
                      "feesUSD": "50"
                    }
                  }
                ]
              }
            }
            """;

        server.expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).hasSize(1);
        PortfolioDTO.PositionDTO pos = positions.get(0);

        assertThat(pos.protocol()).isEqualTo("UniswapV3");
        assertThat(pos.network()).isEqualTo("ethereum");
        assertThat(pos.asset()).isEqualTo("LP-ETH/USDC-500");
        assertThat(pos.amount()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(pos.usdValue()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(pos.apr()).isEqualByComparingTo(new BigDecimal("0.365"));
        assertThat(pos.borrowAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pos.borrowApr()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pos.riskStatus()).isEqualTo("OK");
        assertThat(pos.positionType()).isEqualTo("DEPOSIT");
    }

    @Test
    void returnsEmptyWhenNoPositions() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        UniswapV3Service service = buildService(restTemplate);

        String body = "{\"data\":{\"positions\":[]}}";

        server.expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).isEmpty();
    }
}

