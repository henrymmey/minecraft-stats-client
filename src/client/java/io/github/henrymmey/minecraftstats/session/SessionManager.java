package io.github.henrymmey.minecraftstats.session;

import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.UUID;

public final class SessionManager {
    private ClientSession current;

    public ClientSession update(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getCurrentServer() == null) {
            return null;
        }

        UUID playerUuid = minecraft.player.getUUID();
        String username = minecraft.player.getName().getString();
        String address = minecraft.getCurrentServer().ip;

        ServerAddress parsed = parseAddress(address);

        if (current == null
                || !Objects.equals(current.playerUuid(), playerUuid)
                || !Objects.equals(current.username(), username)
                || !current.serverKey().equalsIgnoreCase(parsed.host + ":" + parsed.port)) {
            current = new ClientSession(
                    UUID.randomUUID(),
                    playerUuid,
                    username,
                    parsed.host,
                    parsed.port
            );
        }

        return current;
    }

    public ClientSession current() {
        return current;
    }

    public ClientSession disconnect() {
        ClientSession previous = current;
        current = null;
        return previous;
    }

    private ServerAddress parseAddress(String address) {
        String host = address;
        int port = 25565;

        int separator = address.lastIndexOf(':');

        if (separator > 0 && address.indexOf(']') < separator) {
            String candidate = address.substring(separator + 1);

            try {
                port = Integer.parseInt(candidate);
                host = address.substring(0, separator);
            } catch (NumberFormatException ignored) {
            }
        }

        return new ServerAddress(host, port);
    }

    private record ServerAddress(String host, int port) {}
}
