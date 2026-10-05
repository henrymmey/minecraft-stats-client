package io.github.henrymmey.minecraftstats.session;

import java.util.UUID;

public record ClientSession(
        UUID id,
        UUID playerUuid,
        String username,
        String serverHost,
        int serverPort
) {
    public String serverKey() {
        return serverHost + ":" + serverPort;
    }
}
