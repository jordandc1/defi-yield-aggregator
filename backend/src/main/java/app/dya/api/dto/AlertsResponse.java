package app.dya.api.dto;

import java.util.List;

public record AlertsResponse(String address, List<AlertItem> alerts) {}
