package io.github.henrymmey.minecraftstats.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IngestBatch(
        int protocolVersion,
        ClientInfo client,
        Player player,
        Server server,
        String season,
        UUID sessionId,
        OffsetDateTime observedAt,
        List<StatObservation> stats,
        List<ClientEvent> events
) {
    public record ClientInfo(
            String modVersion,
            String minecraftVersion,
            String fabricLoaderVersion
    ) {}

    public record Player(UUID uuid, String username) {}
    public record Server(String hostname, int port) {}
}
