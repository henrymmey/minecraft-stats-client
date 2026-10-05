package io.github.henrymmey.minecraftstats.queue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class BatchQueue {
    private final Path directory;
    private final int maxSize;

    public BatchQueue(Path directory, int maxSize) {
        this.directory = directory;
        this.maxSize = Math.max(1, maxSize);

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize upload queue.", exception);
        }
    }

    public synchronized boolean offer(String json) {
        if (size() >= maxSize) return false;

        try {
            Path target = directory.resolve(
                    Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".json"
            );

            Files.writeString(
                    target,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
            );

            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public synchronized Optional<Path> peek() {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .min(Comparator.comparing(path -> path.getFileName().toString()));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public synchronized Optional<String> read(Path path) {
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public synchronized void remove(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    public synchronized int size() {
        try (Stream<Path> files = Files.list(directory)) {
            return (int) files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .count();
        } catch (IOException exception) {
            return 0;
        }
    }
}
