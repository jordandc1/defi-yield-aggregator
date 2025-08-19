package app.dya.service.compound;

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

class CompoundV2ServiceTest {

    private CompoundV2Service buildService(RestTemplate restTemplate) {
        return new CompoundV2Service(new RestTemplateBuilder() {
            @Override
            public RestTemplate build() {
                return restTemplate;
            }
        }, "http://example.com");
    }

    @Test
    void parsesPositionsFromApi() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

        CompoundV2Service service = buildService(restTemplate);

        String body = "{" +
                "\"account\":{\"tokens\":[{" +
                "\"underlyingSymbol\":\"DAI\",\"balanceUnderlying\":\"100\",\"borrowBalanceUnderlying\":\"10\"," +
                "\"supplyRatePerBlock\":\"0.02\",\"borrowRatePerBlock\":\"0.04\",\"underlyingPrice\":\"1\"}]}" +
                "}";

        server.expect(requestTo("http://example.com/0xabc"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).hasSize(2);

        PortfolioDTO.PositionDTO deposit = positions.get(0);
        assertThat(deposit.protocol()).isEqualTo("Compound");
        assertThat(deposit.asset()).isEqualTo("DAI");
        assertThat(deposit.amount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(deposit.usdValue()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(deposit.apr()).isEqualByComparingTo(new BigDecimal("0.02"));
        assertThat(deposit.positionType()).isEqualTo("DEPOSIT");

        PortfolioDTO.PositionDTO borrow = positions.get(1);
        assertThat(borrow.positionType()).isEqualTo("BORROW");
        assertThat(borrow.borrowAmount()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(borrow.borrowApr()).isEqualByComparingTo(new BigDecimal("0.04"));
    }

    @Test
    void returnsEmptyWhenNoTokens() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        CompoundV2Service service = buildService(restTemplate);

        String body = "{\"account\":{\"tokens\":[]}}";

        server.expect(requestTo("http://example.com/0xabc"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).isEmpty();
    }

    @Test
    void handlesBorrowOnly() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        CompoundV2Service service = buildService(restTemplate);

        String body = "{" +
                "\"account\":{\"tokens\":[{" +
                "\"underlyingSymbol\":\"USDC\",\"balanceUnderlying\":\"0\",\"borrowBalanceUnderlying\":\"20\"," +
                "\"supplyRatePerBlock\":\"0\",\"borrowRatePerBlock\":\"0.03\",\"underlyingPrice\":\"1\"}]}" +
                "}";

        server.expect(requestTo("http://example.com/0xabc"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).hasSize(1);
        PortfolioDTO.PositionDTO borrow = positions.get(0);
        assertThat(borrow.positionType()).isEqualTo("BORROW");
        assertThat(borrow.asset()).isEqualTo("USDC");
        assertThat(borrow.borrowAmount()).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void ignoresTokensWithZeroBalances() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        CompoundV2Service service = buildService(restTemplate);

        String body = "{" +
                "\"account\":{\"tokens\":[{" +
                "\"underlyingSymbol\":\"ETH\",\"balanceUnderlying\":\"0\",\"borrowBalanceUnderlying\":\"0\"," +
                "\"supplyRatePerBlock\":\"0.01\",\"borrowRatePerBlock\":\"0.02\",\"underlyingPrice\":\"2000\"}]}" +
                "}";

        server.expect(requestTo("http://example.com/0xabc"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<PortfolioDTO.PositionDTO> positions = service.getPositions("0xabc");
        assertThat(positions).isEmpty();
    }
}

