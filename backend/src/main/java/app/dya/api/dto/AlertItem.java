package app.dya.api.dto;

public record AlertItem(
        String type,     // "HEALTH_FACTOR_LOW", "APY_DROP"
        String message,
        String protocol,
        String createdIso
) {}