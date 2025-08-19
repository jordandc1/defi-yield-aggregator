package app.dya.service.compound;

import app.dya.api.dto.PortfolioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service responsible for fetching positions from the Compound v2 protocol via on-chain calls.
 */
@Service
public class CompoundV2Service {

    private final CompoundLensClient lensClient;
    private final List<TokenMetadata> tokens;

    @Autowired
    public CompoundV2Service(@Value("${app.chains.ethereum.rpcUrl:}") String rpcUrl,
                             @Value("${app.chains.ethereum.infuraApiKey:}") String infuraApiKey) {
        if (rpcUrl == null || rpcUrl.isBlank()) {
            rpcUrl = String.format("https://mainnet.infura.io/v3/%s", infuraApiKey);
        }
        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        this.lensClient = new CompoundLensClient(web3j);
        this.tokens = defaultTokens();
    }

    // package-private for tests
    CompoundV2Service(CompoundLensClient lensClient) {
        this.lensClient = lensClient;
        this.tokens = defaultTokens();
    }

    private List<TokenMetadata> defaultTokens() {
        return List.of(
                new TokenMetadata("DAI", "0x5d3a536e4d6dbd6114cc1ead35777bab948e3643", 18),
                new TokenMetadata("USDC", "0x39AA39c021dfbaE8faC545936693aC917d5E7563", 6)
        );
    }

    /**
     * Retrieve Compound v2 positions for a given wallet address using on-chain data.
     *
     * @param address wallet address
     * @return list of positions held on Compound v2
     */
    public List<PortfolioDTO.PositionDTO> getPositions(String address) {
        if (address == null || address.isBlank()) {
            return Collections.emptyList();
        }
        return positions;
    }

    private List<PortfolioDTO.PositionDTO> mapToken(Map<String, Object> token) {
        // CompoundLens returns more verbose field names. Fall back to the
        // simplified names used in earlier iterations for backward
        // compatibility of tests and potential API shims.
        Object symbolObj = token.getOrDefault("underlyingSymbol", token.get("symbol"));
        if (symbolObj == null) {
            return Collections.emptyList();
        }
        String symbol = symbolObj.toString();

        BigDecimal usdPrice = new BigDecimal(
                token.getOrDefault("underlyingPrice",
                        token.getOrDefault("usdPrice", "0")).toString()
        );

        BigDecimal supplyBalance = new BigDecimal(
                token.getOrDefault("balanceUnderlying",
                        token.getOrDefault("supplyBalanceUnderlying",
                                token.getOrDefault("supplyBalance", "0"))).toString()
        );

        BigDecimal borrowBalance = new BigDecimal(
                token.getOrDefault("borrowBalanceUnderlying",
                        token.getOrDefault("borrowBalance", "0")).toString()
        );

        BigDecimal supplyRate = new BigDecimal(
                token.getOrDefault("supplyRatePerBlock",
                        token.getOrDefault("supplyRate", "0")).toString()
        );

        BigDecimal borrowRate = new BigDecimal(
                token.getOrDefault("borrowRatePerBlock",
                        token.getOrDefault("borrowRate", "0")).toString()
        );

        List<PortfolioDTO.PositionDTO> positions = new ArrayList<>();
        try {
            for (TokenMetadata token : tokens) {
                CompoundLensClient.CTokenBalance balance = lensClient.getBalance(token.cTokenAddress, address);
                BigDecimal supply = toDecimal(balance.supply(), token.decimals);
                BigDecimal borrow = toDecimal(balance.borrow(), token.decimals);

                if (supply.compareTo(BigDecimal.ZERO) > 0) {
                    positions.add(new PortfolioDTO.PositionDTO(
                            "Compound",
                            "ethereum",
                            token.symbol,
                            supply,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "OK",
                            "DEPOSIT"
                    ));
                }

                if (borrow.compareTo(BigDecimal.ZERO) > 0) {
                    positions.add(new PortfolioDTO.PositionDTO(
                            "Compound",
                            "ethereum",
                            token.symbol,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            borrow,
                            BigDecimal.ZERO,
                            "OK",
                            "BORROW"
                    ));
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return positions;
    }

    private BigDecimal toDecimal(BigInteger value, int decimals) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value).movePointLeft(decimals);
    }

    private record TokenMetadata(String symbol, String cTokenAddress, int decimals) {}
}
