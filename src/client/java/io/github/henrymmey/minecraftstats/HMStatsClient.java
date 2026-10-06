package io.github.henrymmey.hmstats;

import io.github.henrymmey.hmstats.api.ApiClient;
import io.github.henrymmey.hmstats.collectors.VanillaStatsCollector;
import io.github.henrymmey.hmstats.config.ClientConfig;
import io.github.henrymmey.hmstats.privacy.ServerFilter;
import io.github.henrymmey.hmstats.queue.BatchQueue;
import io.github.henrymmey.hmstats.session.SessionManager;
import io.github.henrymmey.hmstats.telemetry.TelemetryController;
import io.github.henrymmey.hmstats.upload.BatchUploader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class HMStatsClient implements ClientModInitializer {
    public static final String MOD_ID = "hm-stats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("hm-stats.json");

    public static final Path QUEUE_DIRECTORY =
            FabricLoader.getInstance().getConfigDir().resolve("hm-stats-queue");

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
