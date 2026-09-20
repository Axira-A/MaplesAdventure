package dev.maplesadventure.progression.status;

import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Independent contributions may add, but final amount is capped once per canonical status. */
public record WeaponStatusProfile(List<StatusBuildupComponent> components, StatusWeaponWeightClass weightClass) {
    public WeaponStatusProfile(List<StatusBuildupComponent> components) { this(components,null); }
    public static final WeaponStatusProfile EMPTY=new WeaponStatusProfile(List.of());
    public WeaponStatusProfile { components=List.copyOf(components); if(components.size()>8) throw new IllegalArgumentException("Status component limit"); }
    public WeaponStatusProfile merge(WeaponStatusProfile other) {
        var values=new ArrayList<>(components); values.addAll(other.components); return new WeaponStatusProfile(values,weightClass);
    }
    public WeaponStatusProfile forWeight(StatusWeaponWeightClass weight) {
        return new WeaponStatusProfile(components.stream().map(c->new StatusBuildupComponent(c.type(),
                Math.min(1000,c.baseBuildup()*weight.buildup(c.type())/StatusWeaponWeightClass.NORMAL.buildup(c.type())),c.arcaneScaling(),c.policy())).toList(),weight);
    }
    public WeaponStatusProfile followWeaponArcane() {
        return new WeaponStatusProfile(components.stream().map(c->c.type().allowsArcaneScaling()?
                new StatusBuildupComponent(c.type(),c.baseBuildup(),0,StatusArcaneScalingPolicy.FOLLOW_WEAPON_ARCANE):c).toList(),weightClass);
    }
    public StatusBuildupSnapshot evaluate(int arcane) {
        return evaluate(arcane, 0, 1);
    }
    public StatusBuildupSnapshot evaluate(int arcane, double weaponArcane, double motion) {
        var values=new EnumMap<StatusEffectType,Double>(StatusEffectType.class);
        for(var c:components) values.merge(c.type(),Math.min(StatusBuildupSnapshot.MAX_PER_STATUS,c.amount(arcane,weaponArcane,motion)),(a,b)->Math.min(StatusBuildupSnapshot.MAX_PER_STATUS,a+b));
        return new StatusBuildupSnapshot(values);
    }
    public static WeaponStatusProfile legacy(dev.maplesadventure.progression.weapon.WeaponInfusionBuildup bridge) {
        return bridge.status().map(t->new WeaponStatusProfile(List.of(new StatusBuildupComponent(t,t==StatusEffectType.BLEED?28:30,.30)))).orElse(EMPTY);
    }
    public static WeaponStatusProfile parse(JsonObject statuses) {
        if(statuses.size()>StatusEffectType.values().length) throw new IllegalArgumentException("Status count");
        var values=new ArrayList<StatusBuildupComponent>(); var seen=EnumSet.noneOf(StatusEffectType.class);
        statuses.entrySet().forEach(entry->{
            var type=StatusEffectType.parse(entry.getKey()); if(!seen.add(type)) throw new IllegalArgumentException("Duplicate status");
            var v=entry.getValue().getAsJsonObject(); StatusDefinitions.fields(v,Set.of("base_buildup","arcane_scaling","arcane_policy"));
            double coefficient=StatusDefinitions.number(v,"arcane_scaling",0);
            var policy=v.has("arcane_policy")?StatusArcaneScalingPolicy.valueOf(v.get("arcane_policy").getAsString().toUpperCase(Locale.ROOT)):
                    coefficient==0?StatusArcaneScalingPolicy.NONE:StatusArcaneScalingPolicy.EXPLICIT;
            values.add(new StatusBuildupComponent(type,StatusDefinitions.number(v,"base_buildup",0),coefficient,policy));
        });
        return new WeaponStatusProfile(values);
    }
    public void write(RegistryFriendlyByteBuf b) {
        b.writeBoolean(weightClass!=null); if(weightClass!=null) b.writeEnum(weightClass);
        b.writeVarInt(components.size()); for(var c:components){b.writeEnum(c.type()); b.writeDouble(c.baseBuildup()); b.writeDouble(c.arcaneScaling()); b.writeEnum(c.policy());}
    }
    public static WeaponStatusProfile read(RegistryFriendlyByteBuf b) {
        var weight=b.readBoolean()?b.readEnum(StatusWeaponWeightClass.class):null;
        int count=b.readVarInt(); if(count<0||count>8) throw new IllegalArgumentException("Status component count");
        var values=new ArrayList<StatusBuildupComponent>();
        for(int i=0;i<count;i++) values.add(new StatusBuildupComponent(b.readEnum(StatusEffectType.class),b.readDouble(),b.readDouble(),b.readEnum(StatusArcaneScalingPolicy.class)));
        return new WeaponStatusProfile(values,weight);
    }
}
