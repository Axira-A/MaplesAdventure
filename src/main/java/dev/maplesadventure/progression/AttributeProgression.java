package dev.maplesadventure.progression;

import dev.maplesadventure.config.ProgressionConfig;

public final class AttributeProgression {
    public static int level(PlayerAttributeState state) {
        int[] values = new int[Attribute.values().length];
        for (int index = 0; index < Attribute.values().length; index++)
            values[index] = state.get(Attribute.values()[index]);
        return ProgressionCurve.adventureLevel(values);
    }

    public static long costForNextLevel(int level) {
        return costForNextLevel(level, ProgressionConfig.XP_COST_MULTIPLIER.get());
    }

    /** Explicit server-authored multiplier makes previews safe on a remote client. */
    public static long costForNextLevel(int level, double multiplier) {
        return applyMultiplier(ProgressionCurve.baseCostForNextLevel(level), multiplier);
    }

    public static long costForLevels(int currentLevel, int count) {
        return costForLevels(currentLevel, count, ProgressionConfig.XP_COST_MULTIPLIER.get());
    }

    public static long costForLevels(int currentLevel, int count, double multiplier) {
        if (count <= 0) return 0L;
        long total = 0L;
        for (int index = 0; index < count; index++) {
            long cost = costForNextLevel(currentLevel + index, multiplier);
            if (Long.MAX_VALUE - total < cost) return Long.MAX_VALUE;
            total += cost;
        }
        return total;
    }

    static long applyMultiplier(long baseCost, double multiplier) {
        if (baseCost == Long.MAX_VALUE || !Double.isFinite(multiplier) || multiplier <= 0.0D)
            return Long.MAX_VALUE;
        double scaled = baseCost * multiplier;
        return scaled >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) Math.ceil(scaled));
    }

    private AttributeProgression() {}
}
