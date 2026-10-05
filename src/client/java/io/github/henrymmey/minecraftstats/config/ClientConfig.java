package io.github.henrymmey.minecraftstats.config;

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

            return parsed == null ? defaults() : parsed;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Minecraft Stats configuration.", exception);
        }
    }

    public static void save(Path path, ClientConfig config) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

            Files.writeString(
                    temporary,
                    GSON.toJson(config),
                    StandardCharsets.UTF_8
            );

            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save Minecraft Stats configuration.", exception);
        }
    }
}
