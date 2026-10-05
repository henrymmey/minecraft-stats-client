package io.github.henrymmey.minecraftstats.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ClientEvent(
        UUID id,
        String type,
        OffsetDateTime occurredAt,
        Map<String, Object> payload
) {
}
