package app.dya.price;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnExpression("${app.prices.refreshMinutes:0} > 0")
public class PriceRefreshScheduler {
    private final PriceService priceService;

    public PriceRefreshScheduler(PriceService priceService) {
        this.priceService = priceService;
    }

    @Scheduled(
            fixedRateString = "#{${app.prices.refreshMinutes} * 60 * 1000}",
            initialDelayString = "#{${app.prices.refreshMinutes} * 60 * 1000}"
    )
    public void refresh() {
        List<String> symbols = Arrays.stream(Symbol.values()).map(Enum::name).toList();
        try {
            priceService.getUsdPrices(symbols);
        } catch (Exception ignored) {
            // ignore refresh errors
        }
    }
}
