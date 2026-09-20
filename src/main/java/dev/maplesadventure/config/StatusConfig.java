package dev.maplesadventure.config;
import java.util.*;
import dev.maplesadventure.progression.status.StatusEffectType;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class StatusConfig {
    public static final ModConfigSpec SPEC;
    /** Retained for old config readers only; gameplay uses the canonical four resistance curves. */
    @Deprecated public static final Map<StatusEffectType,ModConfigSpec.DoubleValue> PLAYER_THRESHOLDS;
    static {
        var b=new ModConfigSpec.Builder(); var values=new EnumMap<StatusEffectType,ModConfigSpec.DoubleValue>(StatusEffectType.class);
        for(var type:StatusEffectType.values()) values.put(type,b.comment("Legacy value; ignored since Round 13. Player resistance is attribute-derived.").defineInRange("playerThreshold."+type.id(),160.0,.001,100000.0));
        PLAYER_THRESHOLDS=Map.copyOf(values); SPEC=b.build();
    }
    private StatusConfig() {}
}
