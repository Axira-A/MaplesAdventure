package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EchoServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue CANDIDATE_RANGE;
    public static final ModConfigSpec.IntValue MIN_INTERVAL_SECONDS;
    public static final ModConfigSpec.IntValue MAX_INTERVAL_SECONDS;
    public static final ModConfigSpec.DoubleValue PLAYBACK_DELAY_SECONDS;
    public static final ModConfigSpec.DoubleValue HISTORY_SECONDS;
    public static final ModConfigSpec.IntValue SAMPLE_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue DEBUG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("residualEcho");
        ENABLED = builder.define("residualEchoEnabled", true);
        CANDIDATE_RANGE = builder.defineInRange("echoCandidateRange", 32.0D, 8.0D, 64.0D);
        MIN_INTERVAL_SECONDS = builder.defineInRange("echoMinIntervalSeconds", 15, 5, 300);
        MAX_INTERVAL_SECONDS = builder.defineInRange("echoMaxIntervalSeconds", 45, 5, 600);
        PLAYBACK_DELAY_SECONDS = builder.defineInRange("echoPlaybackDelaySeconds", 2.0D, 0.5D, 10.0D);
        HISTORY_SECONDS = builder.comment("Visual playback duration in seconds.")
                .defineInRange("echoHistorySeconds", 10.0D, 8.0D, 12.0D);
        SAMPLE_INTERVAL_TICKS = builder.defineInRange("echoSampleIntervalTicks", 2, 1, 10);
        DEBUG = builder.comment("Log playback selection/rejection without logging frames.")
                .define("echoDebug", false);
        builder.pop();
        SPEC = builder.build();
    }

    private EchoServerConfig() {}
}
