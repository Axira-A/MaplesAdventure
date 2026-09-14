package dev.maplesadventure.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned policy for fixed encounter spawning. */
public final class EncounterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue MANAGED_SPAWNING;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MANAGED_DIMENSIONS;
    public static final ModConfigSpec.BooleanValue RESPAWN_DEFEATED_BOSSES;
    public static final ModConfigSpec.IntValue ACTIVATION_SCAN_INTERVAL;
    public static final ModConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER_SOLO;
    public static final ModConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER_COOP;
    public static final ModConfigSpec.IntValue MAX_BOSS_ROOM_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_BOSS_ROOM_RADIUS;
    public static final ModConfigSpec.BooleanValue DEBUG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("encounters");
        ENABLED = builder.comment("Enable fixed, phase-owned encounter activation.")
                .define("enabled", true);
        MANAGED_SPAWNING = builder.comment("Block automatic hostile-mob spawning in managed dimensions. This never changes gamerules.")
                .define("encounterManagedSpawning", true);
        MANAGED_DIMENSIONS = builder.comment("Dimension ids whose automatic hostile spawning is managed by encounters.")
                .defineListAllowEmpty("managedDimensions", List.of("minecraft:overworld"),
                        value -> value instanceof String text && net.minecraft.resources.ResourceLocation.tryParse(text) != null);
        RESPAWN_DEFEATED_BOSSES = builder.comment("If false, a defeated boss remains defeated across rests, deaths and restarts.")
                .define("respawnDefeatedBosses", false);
        ACTIVATION_SCAN_INTERVAL = builder.comment("Ticks between spatially-indexed encounter activation scans.")
                .defineInRange("activationScanIntervalTicks", 10, 2, 100);
        BOSS_HEALTH_MULTIPLIER_SOLO = builder.comment("Maximum-health multiplier locked for a solo boss attempt.")
                .defineInRange("bossHealthMultiplierSolo", 1.0D, 0.1D, 20.0D);
        BOSS_HEALTH_MULTIPLIER_COOP = builder.comment("Maximum-health multiplier locked for a host plus one cooperator.")
                .defineInRange("bossHealthMultiplierCoop", 1.5D, 0.1D, 20.0D);
        MAX_BOSS_ROOM_BLOCKS = builder.comment("Legacy compatibility only; fog gates no longer flood-fill boss rooms.")
                .defineInRange("maxBossRoomBlocks", 131072, 1024, 1000000);
        MAX_BOSS_ROOM_RADIUS = builder.comment("Legacy compatibility only; fog gates no longer validate sealed room radius.")
                .defineInRange("maxBossRoomRadius", 128, 16, 512);
        DEBUG = builder.comment("Log encounter transitions and show debug command visuals.")
                .define("encounterDebug", false);
        builder.pop();
        SPEC = builder.build();
    }

    private EncounterConfig() {}
}
