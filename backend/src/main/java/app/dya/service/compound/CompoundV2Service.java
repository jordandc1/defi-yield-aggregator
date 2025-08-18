package app.dya.service.compound;

import app.dya.api.dto.PortfolioDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service responsible for fetching positions from the Compound v2 protocol.
 *
 * <p>The current implementation is a stub that returns an empty list. It can be
 * expanded with real protocol integration in the future.</p>
 */
@Service
public class CompoundV2Service {

    /**
     * Retrieve Compound v2 positions for a given wallet address.
     *
     * @param address wallet address
     * @return list of positions held on Compound v2
     */
    public List<PortfolioDTO.PositionDTO> getPositions(String address) {
        return Collections.emptyList();
    }
}

