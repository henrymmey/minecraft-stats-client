package io.github.henrymmey.minecraftstats.upload;

import java.util.HashMap;
import java.util.Map;

public final class StatsSnapshot {
    private final Map<String, Long> previous = new HashMap<>();

    public Map<String, Long> changed(Map<String, Long> current) {
        Map<String, Long> result = new HashMap<>();

        for (Map.Entry<String, Long> entry : current.entrySet()) {
            Long previousValue = previous.put(entry.getKey(), entry.getValue());

            if (previousValue == null || previousValue.longValue() != entry.getValue()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        for (String key : previous.keySet().stream().toList()) {
            if (!current.containsKey(key)) {
                previous.remove(key);
                result.put(key, 0L);
            }
        }

        return result;
    }
}
