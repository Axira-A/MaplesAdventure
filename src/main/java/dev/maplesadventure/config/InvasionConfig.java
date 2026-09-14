package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class InvasionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue GRACE_SECONDS;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue MATCH_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("invasion");
        ENABLED = builder.define("invasionEnabled", true);
        GRACE_SECONDS = builder.defineInRange("invasionGraceSeconds", 60, 0, 3600);
        COOLDOWN_SECONDS = builder.defineInRange("invasionCooldownSeconds", 300, 0, 86400);
        TIMEOUT_SECONDS = builder.defineInRange("invasionTimeoutSeconds", 600, 30, 7200);
        MATCH_INTERVAL_TICKS = builder.comment("Queue matching cadence; matching builds one host list per pass.")
                .defineInRange("invasionMatchIntervalTicks", 30, 20, 40);
        builder.pop();
        SPEC = builder.build();
    }

    private InvasionConfig() {}
}
