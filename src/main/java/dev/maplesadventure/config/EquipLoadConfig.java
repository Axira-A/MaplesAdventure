package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned encumbrance policy. Accessors clamp again to keep malformed legacy configs safe. */
public final class EquipLoadConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue LIGHT_THRESHOLD, HEAVY_THRESHOLD, OVERLOADED_THRESHOLD;
    public static final ModConfigSpec.DoubleValue LIGHT_MOVEMENT_BONUS, HEAVY_MOVEMENT_PENALTY,
            OVERLOADED_MOVEMENT_PENALTY;
    public static final ModConfigSpec.DoubleValue LIGHT_STAMINA_REGEN, HEAVY_STAMINA_REGEN,
            OVERLOADED_STAMINA_REGEN;
    public static final ModConfigSpec.DoubleValue LIGHT_STAMINA_COST, HEAVY_STAMINA_COST,
            OVERLOADED_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue HEAVY_DODGE_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("equipmentLoad");
        LIGHT_THRESHOLD = b.defineInRange("lightThreshold", 0.30D, 0.05D, 0.60D);
        HEAVY_THRESHOLD = b.defineInRange("heavyThreshold", 0.70D, 0.40D, 0.95D);
        OVERLOADED_THRESHOLD = b.defineInRange("overloadedThreshold", 1.00D, 0.80D, 2.00D);
        LIGHT_MOVEMENT_BONUS = b.defineInRange("lightMovementBonus", 0.03D, 0.0D, 0.08D);
        HEAVY_MOVEMENT_PENALTY = b.defineInRange("heavyMovementPenalty", 0.08D, 0.0D, 0.40D);
        OVERLOADED_MOVEMENT_PENALTY = b.defineInRange("overloadedMovementPenalty", 0.20D, 0.0D, 0.75D);
        LIGHT_STAMINA_REGEN = b.defineInRange("lightStaminaRegenMultiplier", 1.15D, 1.0D, 2.0D);
        HEAVY_STAMINA_REGEN = b.defineInRange("heavyStaminaRegenMultiplier", 0.80D, 0.1D, 1.0D);
        OVERLOADED_STAMINA_REGEN = b.defineInRange("overloadedStaminaRegenMultiplier", 0.60D, 0.1D, 1.0D);
        LIGHT_STAMINA_COST = b.defineInRange("lightStaminaCostMultiplier", 0.85D, 0.25D, 1.0D);
        HEAVY_STAMINA_COST = b.defineInRange("heavyStaminaCostMultiplier", 1.20D, 1.0D, 3.0D);
        OVERLOADED_STAMINA_COST = b.defineInRange("overloadedStaminaCostMultiplier", 1.40D, 1.0D, 4.0D);
        HEAVY_DODGE_DISTANCE = b.defineInRange("heavyDodgeDistanceMultiplier", 0.75D, 0.25D, 1.0D);
        b.pop();
        SPEC = b.build();
    }

    public static double lightThreshold() { return Math.clamp(value(LIGHT_THRESHOLD, 0.30D), 0.05D, 0.60D); }
    public static double heavyThreshold() { return Math.max(lightThreshold(), Math.clamp(value(HEAVY_THRESHOLD, 0.70D), 0.40D, 0.95D)); }
    public static double overloadedThreshold() { return Math.max(heavyThreshold(), Math.clamp(value(OVERLOADED_THRESHOLD, 1.00D), 0.80D, 2.0D)); }
    public static double lightMovementBonus() { return Math.clamp(value(LIGHT_MOVEMENT_BONUS, 0.03D), 0.0D, 0.08D); }
    public static double heavyMovementPenalty() { return Math.clamp(value(HEAVY_MOVEMENT_PENALTY, 0.08D), 0.0D, 0.40D); }
    public static double overloadedMovementPenalty() { return Math.clamp(value(OVERLOADED_MOVEMENT_PENALTY, 0.20D), 0.0D, 0.75D); }
    public static double lightStaminaRegenMultiplier() { return value(LIGHT_STAMINA_REGEN, 1.15D); }
    public static double heavyStaminaRegenMultiplier() { return value(HEAVY_STAMINA_REGEN, 0.80D); }
    public static double overloadedStaminaRegenMultiplier() { return value(OVERLOADED_STAMINA_REGEN, 0.60D); }
    public static double lightStaminaCostMultiplier() { return value(LIGHT_STAMINA_COST, 0.85D); }
    public static double heavyStaminaCostMultiplier() { return value(HEAVY_STAMINA_COST, 1.20D); }
    public static double overloadedStaminaCostMultiplier() { return value(OVERLOADED_STAMINA_COST, 1.40D); }
    public static double heavyDodgeDistanceMultiplier() { return value(HEAVY_DODGE_DISTANCE, 0.75D); }
    private static double value(ModConfigSpec.DoubleValue setting, double fallback) {
        try { return setting.get(); } catch (IllegalStateException unavailable) { return fallback; }
    }
    private EquipLoadConfig() {}
}
