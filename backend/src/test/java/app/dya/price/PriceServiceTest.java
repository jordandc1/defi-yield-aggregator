package app.dya.price;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PriceServiceTest {

    @Test
    void returnsOnlySupportedSymbols() {
        CoinGeckoClient client = mock(CoinGeckoClient.class);
        PriceService service = new PriceService(client, 10);

        Map<String, Map<String, Double>> resp = Map.of(
                "ethereum", Map.of("usd", 123.0)
        );
        when(client.fetchUsdPricesByIds(eq(Set.of("ethereum", "usd-coin")))).thenReturn(resp);

        Map<String, Double> prices = service.getUsdPrices(List.of("eth", "usdc", "doge"));

        assertEquals(1, prices.size());
        assertEquals(123.0, prices.get("ETH"));
        assertFalse(prices.containsKey("USDC"));
        assertFalse(prices.containsKey("doge"));

        verify(client).fetchUsdPricesByIds(eq(Set.of("ethereum", "usd-coin")));
    }

    @Test
    void usesCacheWhenClientThrows() {
        CoinGeckoClient client = mock(CoinGeckoClient.class);
        PriceService service = new PriceService(client, 10);

        when(client.fetchUsdPricesByIds(eq(Set.of("ethereum")))).thenReturn(
                Map.of("ethereum", Map.of("usd", 100.0))
        );

        Map<String, Double> first = service.getUsdPrices(List.of("eth"));
        assertEquals(100.0, first.get("ETH"));

        RestClientResponseException failure = new RestClientResponseException(
                "bad", 500, "bad", null, null, null
        );
        when(client.fetchUsdPricesByIds(anySet())).thenThrow(failure);

        Map<String, Double> result = assertDoesNotThrow(
                () -> service.getUsdPrices(List.of("eth", "doge"))
        );

        assertEquals(1, result.size());
        assertEquals(100.0, result.get("ETH"));

        verify(client, times(1)).fetchUsdPricesByIds(anySet());
    }
}
