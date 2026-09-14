package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EchoClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue ALPHA;
    public static final ModConfigSpec.DoubleValue MODEL_SCALE;
    public static final ModConfigSpec.BooleanValue DEBUG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("residualEcho");
        ENABLED = builder.define("residualEchoEnabled", true);
        ALPHA = builder.defineInRange("echoAlpha", 0.32D, 0.1D, 0.6D);
        MODEL_SCALE = builder.comment("Residual Echo model scale; 1.2 is the normal full-size player baseline.")
                .defineInRange("echoModelScale", 1.2D, 0.75D, 1.5D);
        DEBUG = builder.define("echoDebug", false);
        builder.pop();
        SPEC = builder.build();
    }

    private EchoClientConfig() {}
}
