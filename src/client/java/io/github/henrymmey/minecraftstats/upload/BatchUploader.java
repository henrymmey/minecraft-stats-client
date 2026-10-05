package io.github.henrymmey.minecraftstats.upload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.henrymmey.minecraftstats.MinecraftStatsClient;
import io.github.henrymmey.minecraftstats.api.ApiClient;
import io.github.henrymmey.minecraftstats.model.IngestBatch;
import io.github.henrymmey.minecraftstats.queue.BatchQueue;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BatchUploader {
    private static final Gson GSON = new GsonBuilder().create();

    private final ApiClient api;
    private final BatchQueue queue;
    private final AtomicBoolean flushing = new AtomicBoolean();

    public BatchUploader(ApiClient api, BatchQueue queue) {
        this.api = api;
        this.queue = queue;
    }

    public void enqueue(IngestBatch batch) {
        String json = GSON.toJson(batch);

        if (!queue.offer(json)) {
            MinecraftStatsClient.LOGGER.warn(
                    "Minecraft Stats upload queue is full; dropping one batch."
            );
        }
    }

    public void flush() {
        if (!flushing.compareAndSet(false, true)) {
            return;
        }

        sendNext()
                .whenComplete((ignored, error) -> flushing.set(false));
    }

    private CompletableFuture<Void> sendNext() {
        Optional<Path> path = queue.peek();

        if (path.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Path batchPath = path.get();

        Optional<String> json = queue.read(batchPath);

        if (json.isEmpty()) {
            queue.remove(batchPath);
            return sendNext();
        }

        return api.postBatch(json.get()).thenCompose(result -> {
            switch (result.kind()) {
                case SUCCESS, PERMANENT_FAILURE -> {
                    queue.remove(batchPath);
                }
                case RETRY -> {
                    return CompletableFuture.completedFuture(null);
                }
                case DISABLED, NOT_CONFIGURED, INVALID_URL -> {
                    return CompletableFuture.completedFuture(null);
                }
            }

            return sendNext();
        });
    }
}
