package io.github.henrymmey.minecraftstats.privacy;

import io.github.henrymmey.minecraftstats.config.ClientConfig;

import java.util.Locale;

public final class ServerFilter {
    public boolean allows(ClientConfig config, String host, int port) {
        if (config.servers() == null || config.servers().allow() == null || config.servers().allow().isEmpty()) {
            return true;
        }

        String normalized = host.toLowerCase(Locale.ROOT) + ":" + port;

        return config.servers().allow().stream()
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .anyMatch(value -> value.equals(normalized));
    }
}
