package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import dev.maplesadventure.progression.defense.DefenseMitigationCurve;

public final class EnemyDefenseConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DEFENSE_PRESSURE;
    static {
        var builder = new ModConfigSpec.Builder();
        DEFENSE_PRESSURE = builder.comment("Soft defense pressure. Does not replace Vanilla armor.")
                .defineInRange("defensePressure", DefenseMitigationCurve.DEFAULT_PRESSURE, .05, 2.0);
        SPEC = builder.build();
    }
    private EnemyDefenseConfig() {}
}
