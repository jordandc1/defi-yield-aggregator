package app.dya.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory storage of alert subscription emails keyed by wallet address.
 */
@Service
public class AlertSubscriptionService {

    private final Map<String, String> emailSubscriptions = new ConcurrentHashMap<>();

    public void subscribeEmail(String address, String email) {
        if (address == null || email == null) return;
        emailSubscriptions.put(address.toLowerCase(), email);
    }

    public Optional<String> getEmail(String address) {
        if (address == null) return Optional.empty();
        return Optional.ofNullable(emailSubscriptions.get(address.toLowerCase()));
    }
}
