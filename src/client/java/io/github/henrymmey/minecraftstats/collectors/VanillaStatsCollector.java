package io.github.henrymmey.minecraftstats.collectors;

import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;

import java.util.HashMap;
import java.util.Map;

public final class VanillaStatsCollector {
    public Map<String, Long> collect(StatsCounter counter) {
        Map<String, Long> result = new HashMap<>();

        collectType(counter, Stats.BLOCK_MINED, result);
        collectType(counter, Stats.ITEM_CRAFTED, result);
        collectType(counter, Stats.ITEM_USED, result);
        collectType(counter, Stats.ITEM_BROKEN, result);
        collectType(counter, Stats.ITEM_PICKED_UP, result);
        collectType(counter, Stats.ITEM_DROPPED, result);
        collectType(counter, Stats.ENTITY_KILLED, result);
        collectType(counter, Stats.ENTITY_KILLED_BY, result);
        collectType(counter, Stats.CUSTOM, result);

        return result;
    }

    private <T> void collectType(
            StatsCounter counter,
            StatType<T> type,
            Map<String, Long> result
    ) {
        for (Stat<T> stat : type) {
            String key = Stat.buildName(type, stat.getValue());
            result.put(key, (long) counter.getValue(stat));
        }
    }
}
