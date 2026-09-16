package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlayerDefenseConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue PRESSURE;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.define("playerDefenseEnabled", true);
        PRESSURE = b.comment("Build defense only; Vanilla armor is a separate preceding layer.")
                .defineInRange("playerDefensePressure", .10, .01, 1.0);
        SPEC = b.build();
    }
    private PlayerDefenseConfig() {}
}
