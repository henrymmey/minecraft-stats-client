package io.github.henrymmey.hmstats.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public record ClientConfig(
        boolean enabled,
        Api api,
        Upload upload,
        Servers servers,
        Privacy privacy
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Api(String url, String key) {}
    public record Upload(int intervalSeconds, int batchSize, int maxQueueSize) {}
    public record Servers(List<String> allow) {}
    public record Privacy(boolean sendStatistics, boolean sendAdvancements, boolean sendEvents) {}

    public static ClientConfig defaults() {
        return new ClientConfig(
                true,
                new Api("https://stats.example.com", ""),
                new Upload(60, 100, 5000),
                new Servers(List.of()),
                new Privacy(true, true, true)
        );
    }

    public ClientConfig normalized() {
        ClientConfig defaults = defaults();

        Api normalizedApi = api == null
                ? defaults.api
                : new Api(
                api.url() == null ? defaults.api.url() : api.url(),
                api.key() == null ? "" : api.key()
        );

        Upload normalizedUpload = upload == null
                ? defaults.upload
                : new Upload(
                Math.max(1, upload.intervalSeconds()),
                Math.max(1, Math.min(500, upload.batchSize())),
                Math.max(1, Math.min(5000, upload.maxQueueSize()))
        );

        Servers normalizedServers = servers == null
                ? defaults.servers
                : new Servers(servers.allow() == null ? List.of() : List.copyOf(servers.allow()));

        Privacy normalizedPrivacy = privacy == null
                ? defaults.privacy
                : new Privacy(
                privacy.sendStatistics(),
                privacy.sendAdvancements(),
                privacy.sendEvents()
        );

        return new ClientConfig(enabled, normalizedApi, normalizedUpload, normalizedServers, normalizedPrivacy);
    }

    public static ClientConfig load(Path path) {
        try {
            if (Files.notExists(path)) {
                ClientConfig defaults = defaults();
                save(path, defaults);
                return defaults;
            }

            ClientConfig parsed = GSON.fromJson(
                    Files.readString(path, StandardCharsets.UTF_8),
                    ClientConfig.class
            );

            return parsed == null ? defaults() : parsed.normalized();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load HM Stats configuration.", exception);
        }
    }

    public static void save(Path path, ClientConfig config) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

            Files.writeString(
                    temporary,
                    GSON.toJson(config.normalized()),
                    StandardCharsets.UTF_8
            );

            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save HM Stats configuration.", exception);
        }
    }
}
