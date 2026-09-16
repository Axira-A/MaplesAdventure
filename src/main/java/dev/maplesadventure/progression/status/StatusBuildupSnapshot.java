package dev.maplesadventure.progression.status;

import java.util.*;
import net.minecraft.nbt.*;

/** Immutable launch/hit decision. Never reconstruct from an owner's current attributes. */
public record StatusBuildupSnapshot(Map<StatusEffectType,Double> amounts) {
    public static final StatusBuildupSnapshot EMPTY=new StatusBuildupSnapshot(Map.of());
    public static final double MAX_PER_STATUS=3000;
    public StatusBuildupSnapshot {
        var copy=new EnumMap<StatusEffectType,Double>(StatusEffectType.class);
        amounts.forEach((t,v)->copy.put(t,StatusResistance.bounded(v,0,MAX_PER_STATUS))); amounts=Collections.unmodifiableMap(copy);
    }
    public CompoundTag save() {
        var n=new CompoundTag(); amounts.forEach((t,v)->n.putDouble(t.id(),v)); return n;
    }
    public static StatusBuildupSnapshot load(CompoundTag n) {
        if(n.getAllKeys().size()>4) throw new IllegalArgumentException("Status snapshot count");
        var values=new EnumMap<StatusEffectType,Double>(StatusEffectType.class);
        for(String key:n.getAllKeys()) values.put(StatusEffectType.parse(key),n.getDouble(key));
        return new StatusBuildupSnapshot(values);
    }
}
