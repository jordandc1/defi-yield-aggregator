package app.dya.price;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinGeckoClientTest {

    @Test
    void returnsEmptyMapOnRateLimit() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        CoinGeckoClient client = new CoinGeckoClient(
                "http://localhost", null, null, 1000, 1000, null, 0
        );
        ReflectionTestUtils.setField(client, "rest", builder.build());

        server.expect(MockRestRequestMatchers.anything())
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Map<String, Map<String, Double>> result = client.fetchUsdPricesByIds(Set.of("bitcoin"));
        assertTrue(result.isEmpty());

        server.verify();
    }
}
