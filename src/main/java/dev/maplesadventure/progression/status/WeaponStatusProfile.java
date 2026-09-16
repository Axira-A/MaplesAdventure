package dev.maplesadventure.progression.status;

import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Independent contributions may add, but final amount is capped once per canonical status. */
public record WeaponStatusProfile(List<StatusBuildupComponent> components) {
    public static final WeaponStatusProfile EMPTY=new WeaponStatusProfile(List.of());
    public WeaponStatusProfile { components=List.copyOf(components); if(components.size()>8) throw new IllegalArgumentException("Status component limit"); }
    public WeaponStatusProfile merge(WeaponStatusProfile other) {
        var values=new ArrayList<>(components); values.addAll(other.components); return new WeaponStatusProfile(values);
    }
    public StatusBuildupSnapshot evaluate(int arcane) {
        var values=new EnumMap<StatusEffectType,Double>(StatusEffectType.class);
        for(var c:components) values.merge(c.type(),c.amount(arcane),(a,b)->Math.min(StatusBuildupSnapshot.MAX_PER_STATUS,a+b));
        return new StatusBuildupSnapshot(values);
    }
    public static WeaponStatusProfile legacy(dev.maplesadventure.progression.weapon.WeaponInfusionBuildup bridge) {
        return bridge.status().map(t->new WeaponStatusProfile(List.of(new StatusBuildupComponent(t,30,t==StatusEffectType.BLEED?.55:.45)))).orElse(EMPTY);
    }
    public static WeaponStatusProfile parse(JsonObject statuses) {
        if(statuses.size()>4) throw new IllegalArgumentException("Status count");
        var values=new ArrayList<StatusBuildupComponent>(); var seen=EnumSet.noneOf(StatusEffectType.class);
        statuses.entrySet().forEach(entry->{
            var type=StatusEffectType.parse(entry.getKey()); if(!seen.add(type)) throw new IllegalArgumentException("Duplicate status");
            var v=entry.getValue().getAsJsonObject(); StatusDefinitions.fields(v,Set.of("base_buildup","arcane_scaling"));
            values.add(new StatusBuildupComponent(type,StatusDefinitions.number(v,"base_buildup",0),StatusDefinitions.number(v,"arcane_scaling",0)));
        });
        return new WeaponStatusProfile(values);
    }
    public void write(RegistryFriendlyByteBuf b) {
        b.writeVarInt(components.size()); for(var c:components){b.writeEnum(c.type()); b.writeDouble(c.baseBuildup()); b.writeDouble(c.arcaneScaling());}
    }
    public static WeaponStatusProfile read(RegistryFriendlyByteBuf b) {
        int count=b.readVarInt(); if(count<0||count>8) throw new IllegalArgumentException("Status component count");
        var values=new ArrayList<StatusBuildupComponent>();
        for(int i=0;i<count;i++) values.add(new StatusBuildupComponent(b.readEnum(StatusEffectType.class),b.readDouble(),b.readDouble()));
        return new WeaponStatusProfile(values);
    }
}
