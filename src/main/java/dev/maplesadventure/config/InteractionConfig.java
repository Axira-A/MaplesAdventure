package dev.maplesadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class InteractionConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue INTERACTION_ENABLED;
    public static final ModConfigSpec.IntValue SCAN_INTERVAL;
    public static final ModConfigSpec.DoubleValue MAX_INTERACTION_DISTANCE;
    public static final ModConfigSpec.DoubleValue CANDIDATE_TOLERANCE;
    public static final ModConfigSpec.BooleanValue TARGET_STICKINESS;
    public static final ModConfigSpec.DoubleValue SCREEN_CENTER_WEIGHT;
    public static final ModConfigSpec.DoubleValue FACING_WEIGHT;
    public static final ModConfigSpec.DoubleValue DISTANCE_WEIGHT;
    public static final ModConfigSpec.BooleanValue MARKER_ENABLED;
    public static final ModConfigSpec.BooleanValue PROMPT_ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_TARGET_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_SWITCH_HINT;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.DoubleValue HUD_OPACITY;
    public static final ModConfigSpec.IntValue PROMPT_BOTTOM_OFFSET;
    public static final ModConfigSpec.BooleanValue DEBUG;
    public static final ModConfigSpec.DoubleValue LOST_SOUL_HEAD_ALPHA;
    public static final ModConfigSpec.BooleanValue LOST_SOUL_PARTICLES;
    public static final ModConfigSpec.BooleanValue LOST_SOUL_NAMEPLATE;
    public static final ModConfigSpec.BooleanValue MESSAGE_RUNE_ENABLED;
    public static final ModConfigSpec.DoubleValue MESSAGE_RUNE_ALPHA;
    public static final ModConfigSpec.IntValue MESSAGE_RENDER_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("interaction");
        INTERACTION_ENABLED = builder.define("interactionEnabled", true);
        SCAN_INTERVAL = builder
                .comment("Client ticks between side-effect-free candidate scans.")
                .defineInRange("interactionScanInterval", 3, 1, 20);
        MAX_INTERACTION_DISTANCE = builder
                .comment("Maximum body-to-target-surface distance for contextual interaction. Never expands vanilla/server reach.")
                .defineInRange("maxInteractionDistance", 1.25D, 0.25D, 4.5D);
        CANDIDATE_TOLERANCE = builder
                .comment("Broad-phase scan margin only. It never expands the formal interaction distance.")
                .defineInRange("interactionCandidateTolerance", 0.35D, 0.0D, 1.0D);
        TARGET_STICKINESS = builder.define("interactionTargetStickiness", true);
        builder.pop();

        builder.push("scoring");
        DISTANCE_WEIGHT = builder.defineInRange("distanceWeight", 0.50D, 0.0D, 5.0D);
        FACING_WEIGHT = builder.defineInRange("facingWeight", 0.32D, 0.0D, 5.0D);
        SCREEN_CENTER_WEIGHT = builder.defineInRange("screenCenterWeight", 0.18D, 0.0D, 5.0D);
        builder.pop();

        builder.push("hud");
        MARKER_ENABLED = builder.define("interactionMarkerEnabled", true);
        PROMPT_ENABLED = builder.define("interactionPromptEnabled", true);
        SHOW_TARGET_NAME = builder.define("showTargetName", true);
        SHOW_SWITCH_HINT = builder.define("showSwitchHint", true);
        HUD_SCALE = builder.defineInRange("scale", 1.0D, 0.5D, 2.0D);
        HUD_OPACITY = builder.defineInRange("opacity", 0.88D, 0.1D, 1.0D);
        PROMPT_BOTTOM_OFFSET = builder.defineInRange("promptBottomOffset", 54, 8, 180);
        builder.pop();

        builder.push("development");
        DEBUG = builder
                .comment("Show interaction diagnostics. Registry IDs only appear while this is enabled.")
                .define("interactionDebug", false);
        builder.pop();

        builder.push("lostSoulVisuals");
        LOST_SOUL_HEAD_ALPHA = builder
                .comment("Opacity of the translucent Lost Soul player head.")
                .defineInRange("lostSoulHeadAlpha", 0.68D, 0.2D, 1.0D);
        LOST_SOUL_PARTICLES = builder.define("lostSoulParticleEnabled", true);
        LOST_SOUL_NAMEPLATE = builder.define("lostSoulNameplateEnabled", true);
        builder.pop();

        builder.push("messageVisuals");
        MESSAGE_RUNE_ENABLED = builder.define("messageRuneEnabled", true);
        MESSAGE_RUNE_ALPHA = builder.defineInRange("messageRuneAlpha", 0.88D, 0.2D, 1.0D);
        MESSAGE_RENDER_DISTANCE = builder.defineInRange("messageRenderDistance", 48, 8, 96);
        builder.pop();

        SPEC = builder.build();
    }

    private InteractionConfig() {
    }
}
