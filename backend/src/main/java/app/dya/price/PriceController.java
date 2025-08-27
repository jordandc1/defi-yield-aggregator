package app.dya.price;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/prices")
@CrossOrigin(origins = "*")
public class PriceController {

    private final PriceService svc;
    public PriceController(PriceService svc){ this.svc = svc; }

    /** GET /prices?symbols=ETH,DAI,USDC -> {"ETH":..., "DAI":..., "USDC":...} */
    @GetMapping
    public ResponseEntity<?> getPrices(@RequestParam String symbols){
        try {
            var list = Arrays.asList(symbols.split(","));
            return ResponseEntity.ok(svc.getUsdPrices(list));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        try {
            return ResponseEntity.ok(svc.ping()); // inject client in controller or call via service
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

}
