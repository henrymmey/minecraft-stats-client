package io.github.henrymmey.minecraftstats.telemetry;

import io.github.henrymmey.minecraftstats.MinecraftStatsClient;
import io.github.henrymmey.minecraftstats.collectors.VanillaStatsCollector;
import io.github.henrymmey.minecraftstats.config.ClientConfig;
import io.github.henrymmey.minecraftstats.model.ClientEvent;
import io.github.henrymmey.minecraftstats.model.IngestBatch;
import io.github.henrymmey.minecraftstats.privacy.ServerFilter;
import io.github.henrymmey.minecraftstats.session.ClientSession;
import io.github.henrymmey.minecraftstats.session.SessionManager;
import io.github.henrymmey.minecraftstats.upload.BatchUploader;
import io.github.henrymmey.minecraftstats.upload.StatsSnapshot;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.stats.StatsCounter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TelemetryController {
    private final ClientConfig config;
    private final SessionManager sessions;
    private final VanillaStatsCollector stats;
    private final ServerFilter serverFilter;
    private final StatsSnapshot snapshot = new StatsSnapshot();
    private final BatchUploader uploader;

    private ClientSession lastUploadedSession;
    private int ticksUntilSnapshot;

    public TelemetryController(
            ClientConfig config,
            SessionManager sessions,
            VanillaStatsCollector stats,
            ServerFilter serverFilter,
            BatchUploader uploader
    ) {
        this.config = config;
        this.sessions = sessions;
        this.stats = stats;
        this.serverFilter = serverFilter;
        this.uploader = uploader;
        this.ticksUntilSnapshot = 1;
    }

    public void tick(Minecraft minecraft) {
        uploader.flush();

        if (!config.enabled()) {
            return;
        }

        ClientSession session = sessions.update(minecraft);

        if (session == null) {
            ClientSession ended = sessions.disconnect();

            if (ended != null && lastUploadedSession != null && lastUploadedSession.id().equals(ended.id())) {
                uploader.enqueue(
                        buildBatch(ended, Instant.now().toString()),
                        Map.of(),
                        List.of(event("SESSION_ENDED"))
                );
                lastUploadedSession = null;
            }

            return;
        }

        if (!serverFilter.allows(config, session.serverHost(), session.serverPort())) {
            return;
        }

        if (ticksUntilSnapshot > 0) {
            ticksUntilSnapshot--;
        }

        if (ticksUntilSnapshot > 0 && lastUploadedSession != null
                && lastUploadedSession.id().equals(session.id())) {
            return;
        }

        if (minecraft.player == null) {
            return;
        }

        StatsCounter counter = minecraft.player.getStats();
        Map<String, Long> current = config.privacy().sendStatistics()
                ? stats.collect(counter)
                : Map.of();

        Map<String, Long> changed = snapshot.changed(current);

        List<ClientEvent> events = lastUploadedSession == null
                || !lastUploadedSession.id().equals(session.id())
                ? List.of(event("SESSION_STARTED"))
                : List.of();

        uploader.enqueue(
                buildBatch(session, Instant.now().toString()),
                changed,
                events
        );

        lastUploadedSession = session;
        ticksUntilSnapshot = Math.max(20, config.upload().intervalSeconds() * 20);
    }

    private IngestBatch buildBatch(ClientSession session, String observedAt) {
        return new IngestBatch(
                1,
                new IngestBatch.ClientInfo(
                        modVersion(),
                        minecraftVersion(),
                        fabricLoaderVersion()
                ),
                new IngestBatch.Player(session.playerUuid(), session.username()),
                new IngestBatch.Server(session.serverHost(), session.serverPort()),
                null,
                session.id(),
                observedAt,
                List.of(),
                List.of()
        );
    }

    private ClientEvent event(String type) {
        return new ClientEvent(
                UUID.randomUUID(),
                type,
                Instant.now().toString(),
                Map.of()
        );
    }

    private String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MinecraftStatsClient.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private String minecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(SharedConstants.getCurrentVersion().getName());
    }

    private String fabricLoaderVersion() {
        return FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
