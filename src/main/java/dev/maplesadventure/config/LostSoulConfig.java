package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative Lost Soul rules. */
public final class LostSoulConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue RECOVERY_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("lostSoul");
        ENABLED = builder
                .comment("Move all player death experience into a single recoverable Lost Soul.")
                .define("lostSoulEnabled", true);
        RECOVERY_DISTANCE = builder
                .comment("Maximum body-to-soul distance. Vanilla reach and line of sight still apply.")
                .defineInRange("lostSoulRecoveryDistance", 1.25D, 0.25D, 4.5D);
        builder.pop();
        SPEC = builder.build();
    }

    private LostSoulConfig() {
    }
}
