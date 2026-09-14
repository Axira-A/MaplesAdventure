package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PhaseConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DEBUG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("phase");
        DEBUG = builder.comment("Log phase assignments and rejected cross-phase actions. Never logs every tick.")
                .define("phaseDebug", false);
        builder.pop();
        SPEC = builder.build();
    }

    private PhaseConfig() {}
}
