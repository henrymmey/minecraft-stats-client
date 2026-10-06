package io.github.henrymmey.hmstats.api;

import io.github.henrymmey.hmstats.config.ClientConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class ApiClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ClientConfig config;

    public ApiClient(ClientConfig config) {
        this.config = config;
    }

    public CompletableFuture<ApiResult> postBatch(String json) {
        if (!config.enabled()) return CompletableFuture.completedFuture(ApiResult.disabled());

        String url = config.api().url();
        String key = config.api().key();

        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            return CompletableFuture.completedFuture(ApiResult.notConfigured());
        }

        final URI uri;
        try {
            uri = URI.create(url.replaceAll("/+$", "") + "/api/v1/ingest/batch");
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !("http".equalsIgnoreCase(uri.getScheme()) && isLocalHost(uri.getHost()))) {
                return CompletableFuture.completedFuture(ApiResult.invalidUrl());
            }
        } catch (IllegalArgumentException exception) {
            return CompletableFuture.completedFuture(ApiResult.invalidUrl());
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();

                    if (status >= 200 && status < 300) return ApiResult.success(status);
                    if (status == 408 || status == 429 || status >= 500) return ApiResult.retry(status);

                    return ApiResult.permanentFailure(status);
                })
                .exceptionally(ignored -> ApiResult.retry(-1));
    }

    private boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    public record ApiResult(Kind kind, int httpStatus) {
        public enum Kind {
            SUCCESS, RETRY, PERMANENT_FAILURE, DISABLED, NOT_CONFIGURED, INVALID_URL
        }

        static ApiResult success(int status) { return new ApiResult(Kind.SUCCESS, status); }
        static ApiResult retry(int status) { return new ApiResult(Kind.RETRY, status); }
        static ApiResult permanentFailure(int status) { return new ApiResult(Kind.PERMANENT_FAILURE, status); }
        static ApiResult disabled() { return new ApiResult(Kind.DISABLED, 0); }
        static ApiResult notConfigured() { return new ApiResult(Kind.NOT_CONFIGURED, 0); }
        static ApiResult invalidUrl() { return new ApiResult(Kind.INVALID_URL, 0); }
    }
}
