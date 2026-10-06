package io.github.henrymmey.hmstats.modmenu;

import io.github.henrymmey.hmstats.HMStatsClient;
import io.github.henrymmey.hmstats.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public final class HMStatsConfigScreen extends Screen {
    private static final int FIELD_WIDTH = 360;
    private static final int FIELD_HEIGHT = 20;

    private final Screen parent;

    private EditBox apiUrlField;
    private EditBox apiKeyField;
    private EditBox intervalField;
    private EditBox batchSizeField;
    private EditBox maxQueueField;
    private EditBox allowedServersField;

    private boolean enabled;
    private boolean sendStatistics;
    private boolean sendEvents;

    private Component error;
    private Component connectionStatus;
    private boolean testingConnection;
    private Button testConnectionButton;

    public HMStatsConfigScreen(Screen parent) {
        super(Component.literal("HM Stats Configuration"));
        this.parent = parent;

        ClientConfig config = HMStatsClient.config().normalized();
        this.enabled = config.enabled();
        this.sendStatistics = config.privacy().sendStatistics();
        this.sendEvents = config.privacy().sendEvents();
    }

    @Override
    protected void init() {
        super.init();

        ClientConfig config = HMStatsClient.config().normalized();

        int left = (this.width - FIELD_WIDTH) / 2;
        int y = 54;

        addRenderableWidget(CycleButton.<Boolean>builder(
                        value -> Component.literal(value ? "On" : "Off"),
                        () -> this.enabled
                )
                .withValues(true, false)
                .create(
                        left,
                        y,
                        FIELD_WIDTH,
                        FIELD_HEIGHT,
                        Component.literal("Enable HM Stats"),
                        (button, value) -> this.enabled = value
                ));

        y += 36;
        this.apiUrlField = addField(left, y, "API URL", config.api().url(), 2048);

        y += 36;
        this.apiKeyField = addField(left, y, "API key", config.api().key(), 4096);
        this.apiKeyField.setSuggestion("Paste your HM Stats ingest key");
        this.apiKeyField.setValue(config.api().key() == null ? "" : config.api().key());
        this.apiKeyField.setMaxLength(4096);

        y += 28;
        this.testConnectionButton = addRenderableWidget(Button.builder(
                Component.literal("Test connection"),
                button -> testConnection()
        ).bounds(left, y, FIELD_WIDTH, FIELD_HEIGHT).build());

        y += 28;
        this.intervalField = addField(
                left,
                y,
                "Upload interval (seconds)",
                Integer.toString(config.upload().intervalSeconds()),
                6
        );

        y += 36;
        this.batchSizeField = addField(
                left,
                y,
                "Batch size",
                Integer.toString(config.upload().batchSize()),
                4
        );

        y += 36;
        this.maxQueueField = addField(
                left,
                y,
                "Maximum queued batches",
                Integer.toString(config.upload().maxQueueSize()),
                5
        );

        y += 36;
        addRenderableWidget(CycleButton.<Boolean>builder(
                        value -> Component.literal(value ? "On" : "Off"),
                        () -> this.sendStatistics
                )
                .withValues(true, false)
                .create(
                        left,
                        y,
                        FIELD_WIDTH,
                        FIELD_HEIGHT,
                        Component.literal("Send statistics"),
                        (button, value) -> this.sendStatistics = value
                ));

        y += 28;
        addRenderableWidget(CycleButton.<Boolean>builder(
                        value -> Component.literal(value ? "On" : "Off"),
                        () -> this.sendEvents
                )
                .withValues(true, false)
                .create(
                        left,
                        y,
                        FIELD_WIDTH,
                        FIELD_HEIGHT,
                        Component.literal("Send session events"),
                        (button, value) -> this.sendEvents = value
                ));

        y += 36;
        this.allowedServersField = addField(
                left,
                y,
                "Allowed servers",
                String.join(", ", config.servers().allow()),
                4096
        );

        int buttonY = Math.min(this.height - 45, y + 36);
        addRenderableWidget(Button.builder(
                Component.literal("Save"),
                button -> save()
        ).bounds(left, buttonY, 174, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> onClose()
        ).bounds(left + 186, buttonY, 174, 20).build());

        if (apiUrlField != null) {
            apiUrlField.setSuggestion("https://stats.example.com");
        }
        if (allowedServersField != null) {
            allowedServersField.setSuggestion("play.example.com:25565, mc.example.net:25565");
        }

        setInitialFocus(apiUrlField);
    }

    private EditBox addField(int x, int y, String label, String value, int maxLength) {
        EditBox field = new EditBox(
                this.font,
                x,
                y,
                FIELD_WIDTH,
                FIELD_HEIGHT,
                Component.literal(label)
        );
        field.setMaxLength(maxLength);
        field.setValue(value == null ? "" : value);
        addRenderableWidget(field);
        return field;
    }

    private void save() {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            this.error = Component.literal(validation.error());
            return;
        }

        try {
            ClientConfig current = HMStatsClient.config().normalized();

            ClientConfig updated = new ClientConfig(
                    enabled,
                    new ClientConfig.Api(
                            apiUrlField.getValue().trim(),
                            apiKeyField.getValue()
                    ),
                    new ClientConfig.Upload(
                            Integer.parseInt(intervalField.getValue().trim()),
                            Integer.parseInt(batchSizeField.getValue().trim()),
                            Integer.parseInt(maxQueueField.getValue().trim())
                    ),
                    new ClientConfig.Servers(parseServers(allowedServersField.getValue())),
                    new ClientConfig.Privacy(
                            sendStatistics,
                            current.privacy().sendAdvancements(),
                            sendEvents
                    )
            ).normalized();

            ClientConfig.save(HMStatsClient.CONFIG_FILE, updated);
            HMStatsClient.applyConfig(updated);

            this.minecraft.gui.setScreen(parent);
        } catch (Exception exception) {
            HMStatsClient.LOGGER.error("Failed to save HM Stats configuration.", exception);
            this.error = Component.literal("Could not save the configuration. Check the log for details.");
        }
    }

    private void testConnection() {
        if (testingConnection) {
            return;
        }

        String url = apiUrlField.getValue().trim();
        String key = apiKeyField.getValue();

        if (url.isBlank()) {
            connectionStatus = Component.literal("Enter an API URL first.");
            return;
        }
        if (key.isBlank()) {
            connectionStatus = Component.literal("Enter an API key first.");
            return;
        }

        try {
            URI uri = URI.create(url);
            boolean secure = "https".equalsIgnoreCase(uri.getScheme());
            boolean localHttp = "http".equalsIgnoreCase(uri.getScheme()) && isLocalHost(uri.getHost());

            if (!secure && !localHttp) {
                connectionStatus = Component.literal("Use HTTPS (HTTP is allowed only for localhost).");
                return;
            }

            testingConnection = true;
            testConnectionButton.active = false;
            connectionStatus = Component.literal("Testing connection...");

            java.net.http.HttpClient.newHttpClient()
                    .sendAsync(
                            java.net.http.HttpRequest.newBuilder(
                                            URI.create(url.replaceAll("/+$", "") + "/api/health")
                                    )
                                    .timeout(java.time.Duration.ofSeconds(10))
                                    .header("Authorization", "Bearer " + key)
                                    .header("Accept", "application/json")
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString()
                    )
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            finishConnectionTest(Component.literal("Connection failed: " + throwable.getClass().getSimpleName()));
                            return;
                        }

                        int status = response.statusCode();
                        if (status >= 200 && status < 300) {
                            finishConnectionTest(Component.literal("Connection successful."));
                        } else if (status == 401 || status == 403) {
                            finishConnectionTest(Component.literal("Server reachable, but the API key was rejected."));
                        } else if (status == 404) {
                            finishConnectionTest(Component.literal("Server reachable, but the health endpoint was not found."));
                        } else {
                            finishConnectionTest(Component.literal("Server responded with HTTP " + status + "."));
                        }
                    });
        } catch (IllegalArgumentException exception) {
            connectionStatus = Component.literal("API URL is not valid.");
        }
    }

    private void finishConnectionTest(Component status) {
        if (this.minecraft == null) {
            return;
        }

        this.minecraft.execute(() -> {
            this.connectionStatus = status;
            this.testingConnection = false;
            if (this.testConnectionButton != null) {
                this.testConnectionButton.active = true;
            }
        });
    }

    private ValidationResult validate() {
        String url = apiUrlField == null ? "" : apiUrlField.getValue().trim();

        if (!url.isEmpty()) {
            try {
                URI uri = URI.create(url);
                boolean secure = "https".equalsIgnoreCase(uri.getScheme());
                boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                        && isLocalHost(uri.getHost());

                if (!secure && !localHttp) {
                    return ValidationResult.invalid(
                            "API URL must use HTTPS (HTTP is allowed only for localhost)."
                    );
                }
            } catch (IllegalArgumentException exception) {
                return ValidationResult.invalid("API URL is not valid.");
            }
        } else if (enabled) {
            return ValidationResult.invalid("Enter an API URL or disable HM Stats.");
        }

        int interval;
        int batchSize;
        int maxQueue;

        try {
            interval = Integer.parseInt(intervalField.getValue().trim());
            batchSize = Integer.parseInt(batchSizeField.getValue().trim());
            maxQueue = Integer.parseInt(maxQueueField.getValue().trim());
        } catch (NumberFormatException exception) {
            return ValidationResult.invalid("Upload values must be whole numbers.");
        }

        if (interval < 1 || interval > 86400) {
            return ValidationResult.invalid("Upload interval must be between 1 and 86400 seconds.");
        }
        if (batchSize < 1 || batchSize > 500) {
            return ValidationResult.invalid("Batch size must be between 1 and 500.");
        }
        if (maxQueue < 1 || maxQueue > 5000) {
            return ValidationResult.invalid("Maximum queued batches must be between 1 and 5000.");
        }

        return ValidationResult.success();
    }

    private List<String> parseServers(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList();
    }

    private boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - FIELD_WIDTH) / 2;

        graphics.text(this.font, "API URL", left, 43, 0xFFFFFFFF, true);
        graphics.text(this.font, "API key", left, 79, 0xFFFFFFFF, true);
        graphics.text(this.font, "Upload interval (seconds)", left, 151, 0xFFFFFFFF, true);
        graphics.text(this.font, "Batch size", left, 187, 0xFFFFFFFF, true);
        graphics.text(this.font, "Maximum queued batches", left, 223, 0xFFFFFFFF, true);
        graphics.text(this.font, "Allowed servers (comma-separated)", left, 359, 0xFFFFFFFF, true);

        if (this.connectionStatus != null) {
            graphics.text(this.font, this.connectionStatus.getString(), left, Math.min(this.height - 85, 455), 0xFFFFFFFF, true);
        }

        if (this.error != null) {
            graphics.text(
                    this.font,
                    this.error.getString(),
                    left,
                    Math.min(this.height - 65, 475),
                    0xFFFF5555,
                    true
            );
        }
    }

    private record ValidationResult(boolean isValid, String error) {
        static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
    }
}
