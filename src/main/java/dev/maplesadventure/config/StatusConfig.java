package dev.maplesadventure.config;
import java.util.*;
import dev.maplesadventure.progression.status.StatusEffectType;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class StatusConfig {
    public static final ModConfigSpec SPEC;
    public static final Map<StatusEffectType,ModConfigSpec.DoubleValue> PLAYER_THRESHOLDS;
    static {
        var b=new ModConfigSpec.Builder(); var values=new EnumMap<StatusEffectType,ModConfigSpec.DoubleValue>(StatusEffectType.class);
        for(var type:StatusEffectType.values()) values.put(type,b.defineInRange("playerThreshold."+type.id(),100.0,.001,100000.0));
        PLAYER_THRESHOLDS=Map.copyOf(values); SPEC=b.build();
    }
    private StatusConfig() {}
}
