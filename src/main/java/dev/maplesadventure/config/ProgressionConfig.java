package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned progression policy. Curve coefficients remain centralized in ProgressionCurve. */
public final class ProgressionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue XP_COST_MULTIPLIER;
    public static final ModConfigSpec.IntValue ATTRIBUTE_HARD_CAP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("progression");
        XP_COST_MULTIPLIER = builder.comment("Multiplier applied after the base next-level XP cost is rounded.")
                .defineInRange("xpCostMultiplier", 1.0D, 0.01D, 100.0D);
        ATTRIBUTE_HARD_CAP = builder.comment("Server cap for each base attribute; the absolute schema cap is 99.")
                .defineInRange("attributeHardCap", 99, 5, 99);
        builder.pop();
        SPEC = builder.build();
    }

    private ProgressionConfig() {}
}
