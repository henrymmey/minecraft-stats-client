package io.github.henrymmey.hmstats.model;

import java.util.Map;
import java.util.UUID;

public record ClientEvent(
        UUID id,
        String type,
        String occurredAt,
        Map<String, Object> payload
) {
}
