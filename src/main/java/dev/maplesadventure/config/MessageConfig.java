package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative creation, retention, synchronization, and read rules. */
public final class MessageConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue MIN_SPACING;
    public static final ModConfigSpec.IntValue CREATION_COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue LIFETIME_DAYS;
    public static final ModConfigSpec.IntValue SYNC_RADIUS;
    public static final ModConfigSpec.DoubleValue READ_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("messages");
        ENABLED = builder.define("enabled", true);
        MAX_ACTIVE_PER_PLAYER = builder.defineInRange("maxActiveMessagesPerPlayer", 15, 1, 100);
        MIN_SPACING = builder.defineInRange("minMessageSpacing", 2.5D, 0.5D, 16.0D);
        CREATION_COOLDOWN_SECONDS = builder.defineInRange("messageCreationCooldownSeconds", 15, 0, 3600);
        LIFETIME_DAYS = builder.defineInRange("messageLifetimeDays", 30, 1, 3650);
        SYNC_RADIUS = builder.defineInRange("nearbySyncRadius", 64, 16, 128);
        READ_DISTANCE = builder
                .comment("Server-authoritative body-to-rune read distance; vanilla reach is still an upper bound.")
                .defineInRange("messageReadDistance", 1.25D, 0.25D, 4.5D);
        builder.pop();
        SPEC = builder.build();
    }

    private MessageConfig() {
    }
}
