package app.dya.api;

import app.dya.api.dto.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "http://localhost:5173")
public class AlertsController {

    @GetMapping("/{address}")
    public AlertsResponse getAlerts(@PathVariable String address) {
        // TODO: implement rules & state
        return new AlertsResponse(
                address,
                List.of(new AlertItem("HEALTH_FACTOR_LOW",
                        "Health factor below 1.3 on Aave position",
                        "Aave",
                        Instant.now().toString()))
        );
    }
}
