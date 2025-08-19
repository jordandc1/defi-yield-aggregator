package app.dya.service.compound;

import app.dya.api.dto.PortfolioDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CompoundV2ServiceTest {

    private static final String ADDRESS = "0xabc";
    private static final String CDAI = "0x5d3a536e4d6dbd6114cc1ead35777bab948e3643";
    private static final String CUSDC = "0x39AA39c021dfbaE8faC545936693aC917d5E7563";


    String body = "{" +
            "\"account\":{\"tokens\":[{" +
            "\"underlyingSymbol\":\"DAI\",\"balanceUnderlying\":\"100\",\"borrowBalanceUnderlying\":\"10\"," +
            "\"supplyRatePerBlock\":\"0.02\",\"borrowRatePerBlock\":\"0.04\",\"underlyingPrice\":\"1\"}]}" +
            "}";

    private CompoundV2Service buildService(CompoundLensClient lens) {
        return new CompoundV2Service(lens);
    }


    private BigInteger toWei(String amount, int decimals) {
        return new BigDecimal(amount).movePointRight(decimals).toBigIntegerExact();
    }

    @Test
    void parsesPositionsFromOnChain() throws Exception {
        CompoundLensClient lens = Mockito.mock(CompoundLensClient.class);
        when(lens.getBalance(CDAI, ADDRESS))
                .thenReturn(new CompoundLensClient.CTokenBalance(toWei("100", 18), toWei("10", 18)));
        when(lens.getBalance(CUSDC, ADDRESS))
                .thenReturn(new CompoundLensClient.CTokenBalance(BigInteger.ZERO, BigInteger.ZERO));

        CompoundV2Service service = buildService(lens);
        List<PortfolioDTO.PositionDTO> positions = service.getPositions(ADDRESS);
        assertThat(positions).hasSize(2);

        PortfolioDTO.PositionDTO deposit = positions.get(0);
        assertThat(deposit.asset()).isEqualTo("DAI");
        assertThat(deposit.amount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(deposit.positionType()).isEqualTo("DEPOSIT");

        PortfolioDTO.PositionDTO borrow = positions.get(1);
        assertThat(borrow.positionType()).isEqualTo("BORROW");
        assertThat(borrow.asset()).isEqualTo("DAI");
        assertThat(borrow.borrowAmount()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void returnsEmptyWhenNoTokens() throws Exception {
        CompoundLensClient lens = Mockito.mock(CompoundLensClient.class);
        when(lens.getBalance(anyString(), eq(ADDRESS)))
                .thenReturn(new CompoundLensClient.CTokenBalance(BigInteger.ZERO, BigInteger.ZERO));

        CompoundV2Service service = buildService(lens);
        List<PortfolioDTO.PositionDTO> positions = service.getPositions(ADDRESS);
        assertThat(positions).isEmpty();
    }

    @Test
    void handlesBorrowOnly() throws Exception {
        CompoundLensClient lens = Mockito.mock(CompoundLensClient.class);
        when(lens.getBalance(CDAI, ADDRESS))
                .thenReturn(new CompoundLensClient.CTokenBalance(BigInteger.ZERO, BigInteger.ZERO));
        when(lens.getBalance(CUSDC, ADDRESS))
                .thenReturn(new CompoundLensClient.CTokenBalance(BigInteger.ZERO, toWei("20", 6)));

        CompoundV2Service service = buildService(lens);
        List<PortfolioDTO.PositionDTO> positions = service.getPositions(ADDRESS);

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
