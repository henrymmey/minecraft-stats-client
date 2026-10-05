package io.github.henrymmey.minecraftstats;

import io.github.henrymmey.minecraftstats.api.ApiClient;
import io.github.henrymmey.minecraftstats.collectors.VanillaStatsCollector;
import io.github.henrymmey.minecraftstats.config.ClientConfig;
import io.github.henrymmey.minecraftstats.privacy.ServerFilter;
import io.github.henrymmey.minecraftstats.queue.BatchQueue;
import io.github.henrymmey.minecraftstats.session.SessionManager;
import io.github.henrymmey.minecraftstats.telemetry.TelemetryController;
import io.github.henrymmey.minecraftstats.upload.BatchUploader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class MinecraftStatsClient implements ClientModInitializer {
    public static final String MOD_ID = "minecraft-stats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("minecraft-stats.json");

    public static final Path QUEUE_DIRECTORY =
            FabricLoader.getInstance().getConfigDir().resolve("minecraft-stats-queue");

    private static ClientConfig config;
    private static BatchQueue queue;
    private static ApiClient apiClient;

    @Override
    public void onInitializeClient() {
        config = ClientConfig.load(CONFIG_FILE);
        queue = new BatchQueue(QUEUE_DIRECTORY, config.upload().maxQueueSize());
        apiClient = new ApiClient(config);

        BatchUploader uploader = new BatchUploader(apiClient, queue, config);
        TelemetryController telemetry = new TelemetryController(
                config,
                new SessionManager(),
                new VanillaStatsCollector(),
                new ServerFilter(),
                uploader
        );

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                telemetry::tick
        );

        LOGGER.info("{} client initialized. Configuration: {}", MOD_ID, CONFIG_FILE);
    }

    public static ClientConfig config() {
        return config;
    }

    public static BatchQueue queue() {
        return queue;
    }

    public static ApiClient api() {
        return apiClient;
    }
}
