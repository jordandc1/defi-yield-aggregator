package app.dya.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service responsible for interactions with the Aave V3 protocol.
 *
 * <p>This is currently a minimal stub and should be expanded with real
 * integration logic. For now it simply returns a default health factor
 * allowing the rest of the application to be wired and tested.</p>
 */
@Service
public class AaveV3Service {

    /**
     * Retrieve the health factor for a wallet. The health factor indicates the
     * safety of a user's borrow position on Aave, where values below 1.0 are
     * subject to liquidation.
     *
     * @param address the wallet address to query
     * @return the health factor as reported by Aave
     */
    public BigDecimal getHealthFactor(String address) {
        // TODO: implement integration with Aave to retrieve actual value
        return BigDecimal.ONE;
    }
}

