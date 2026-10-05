package io.github.henrymmey.minecraftstats.model;

import java.util.Map;
import java.util.UUID;

public record ClientEvent(
        UUID id,
        String type,
        String occurredAt,
        Map<String, Object> payload
) {
}
