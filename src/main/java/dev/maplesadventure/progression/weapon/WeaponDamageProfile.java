package dev.maplesadventure.progression.weapon;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record WeaponDamageProfile(List<WeaponDamageComponent> components,String source,String weaponClass,String debugReason) {
    public static final WeaponDamageProfile STANDARD=new WeaponDamageProfile(List.of(WeaponDamageComponent.inherit(WeaponDamageChannel.PHYSICAL)),"DEFAULT","PHYSICAL","");
    public WeaponDamageProfile {
        components=List.copyOf(components);
        if(components.isEmpty()||components.size()>8) throw new IllegalArgumentException("Damage components must be 1..8");
        Set<WeaponDamageChannel> seen=EnumSet.noneOf(WeaponDamageChannel.class); double sum=0;
        for(var c:components) { if(!seen.add(c.channel())) throw new IllegalArgumentException("Duplicate channel"); sum+=c.baseRatio(); }
        if(!Double.isFinite(sum)||sum<=0||sum>2) throw new IllegalArgumentException("Total base_ratio outside (0,2]");
        if(source==null||source.length()>32||weaponClass==null||weaponClass.length()>64||debugReason==null||debugReason.length()>512)
            throw new IllegalArgumentException("Damage metadata bounds");
    }
    public static WeaponDamageProfile automatic(WeaponRequirementArchetype type) {
        return new WeaponDamageProfile(List.of(WeaponDamageComponent.inherit(WeaponDamageChannel.automatic(type))),"ARCHETYPE",type.name(),"");
    }
    public static WeaponDamageProfile read(RegistryFriendlyByteBuf b) {
        int count=b.readVarInt(); if(count<1||count>8) throw new IllegalArgumentException("Damage component count");
        List<WeaponDamageComponent> components=new ArrayList<>();
        for(int i=0;i<count;i++) {
            int channelId=b.readVarInt();
            if(channelId<0||channelId>=WeaponDamageChannel.values().length) throw new IllegalArgumentException("Channel ID bounds");
            var channel=WeaponDamageChannel.values()[channelId]; double ratio=b.readDouble();
            int modeId=b.readVarInt();
            if(modeId<0||modeId>=WeaponDamageComponent.ScalingMode.values().length) throw new IllegalArgumentException("Scaling mode bounds");
            var mode=WeaponDamageComponent.ScalingMode.values()[modeId];
            components.add(new WeaponDamageComponent(channel,ratio,mode,mode==WeaponDamageComponent.ScalingMode.OVERRIDE?WeaponRequirementNetwork.readScaling(b):null));
        }
        return new WeaponDamageProfile(components,b.readUtf(32),b.readUtf(64),b.readUtf(512));
    }
    public void write(RegistryFriendlyByteBuf b) {
        b.writeVarInt(components.size());
        for(var c:components) {
            b.writeEnum(c.channel()); b.writeDouble(c.baseRatio()); b.writeEnum(c.scalingMode());
            if(c.scalingMode()==WeaponDamageComponent.ScalingMode.OVERRIDE) WeaponRequirementNetwork.writeScaling(b,c.optionalScalingProfile());
        }
        b.writeUtf(source,32); b.writeUtf(weaponClass,64); b.writeUtf(debugReason,512);
    }
}
