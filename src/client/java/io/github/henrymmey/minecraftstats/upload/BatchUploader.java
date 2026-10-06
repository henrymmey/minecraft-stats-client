package io.github.henrymmey.hmstats.upload;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.henrymmey.hmstats.HMStatsClient;
import io.github.henrymmey.hmstats.api.ApiClient;
import io.github.henrymmey.hmstats.config.ClientConfig;
import io.github.henrymmey.hmstats.model.ClientEvent;
import io.github.henrymmey.hmstats.model.IngestBatch;
import io.github.henrymmey.hmstats.model.StatObservation;
import io.github.henrymmey.hmstats.queue.BatchQueue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BatchUploader {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final ApiClient api;
    private final BatchQueue queue;
    private final int batchSize;
    private final AtomicBoolean flushing = new AtomicBoolean();

    private volatile long nextAttemptAtMillis = 0L;
    private volatile int retryCount = 0;
    private volatile boolean blocked = false;

    public BatchUploader(ApiClient api, BatchQueue queue, ClientConfig config) {
        this.api = api;
        this.queue = queue;
        this.batchSize = Math.max(1, Math.min(500, config.upload().batchSize()));
    }

    public void enqueue(IngestBatch base, Map<String, Long> changedStats, List<ClientEvent> events) {
        List<StatObservation> observations = changedStats.entrySet().stream()
                .map(entry -> new StatObservation(entry.getKey(), entry.getValue()))
                .toList();

        if (observations.isEmpty()) {
            enqueueBatch(base, List.of(), events);
            return;
        }

        for (int start = 0; start < observations.size(); start += batchSize) {
            int end = Math.min(start + batchSize, observations.size());

            enqueueBatch(
                    base,
                    observations.subList(start, end),
                    start == 0 ? events : List.of()
            );
        }
    }

    private void enqueueBatch(IngestBatch base, List<StatObservation> stats, List<ClientEvent> events) {
        IngestBatch batch = new IngestBatch(
                base.protocolVersion(),
                base.client(),
                base.player(),
                base.server(),
                base.season(),
                base.sessionId(),
                base.observedAt(),
                stats,
                events
        );

        if (!queue.offer(GSON.toJson(batch))) {
            HMStatsClient.LOGGER.warn(
                    "HM Stats upload queue is full; dropping one batch."
            );
        } else {
            blocked = false;
        }
    }

    public void flush() {
        if (blocked || System.currentTimeMillis() < nextAttemptAtMillis) return;
        if (!flushing.compareAndSet(false, true)) return;

        sendNext()
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        HMStatsClient.LOGGER.debug(
                                "HM Stats queue flush failed.",
                                error
                        );
                    }
                    flushing.set(false);
                });
    }

    private CompletableFuture<Void> sendNext() {
        var path = queue.peek();

        if (path.isEmpty()) {
            retryCount = 0;
            return CompletableFuture.completedFuture(null);
        }

        Path batchPath = path.get();
        var json = queue.read(batchPath);

        if (json.isEmpty()) {
            queue.remove(batchPath);
            return sendNext();
        }

        return api.postBatch(json.get()).thenCompose(result -> {
            switch (result.kind()) {
                case SUCCESS -> {
                    queue.remove(batchPath);
                    retryCount = 0;
                    nextAttemptAtMillis = 0L;
                    return sendNext();
                }

                case RETRY -> {
                    retryCount = Math.min(retryCount + 1, 7);
                    long delaySeconds = 1L << retryCount;
                    nextAttemptAtMillis = System.currentTimeMillis()
                            + Math.min(delaySeconds, 120L) * 1000L;
                    return CompletableFuture.completedFuture(null);
                }

                case PERMANENT_FAILURE, NOT_CONFIGURED, INVALID_URL -> {
                    blocked = true;
                    HMStatsClient.LOGGER.warn(
                            "HM Stats upload paused because the queued request cannot be accepted. HTTP status: {}. The queued data was kept.",
                            result.httpStatus()
                    );
                    return CompletableFuture.completedFuture(null);
                }

                case DISABLED -> {
                    blocked = true;
                    return CompletableFuture.completedFuture(null);
                }
            }

            return CompletableFuture.completedFuture(null);
        });
    }
}
