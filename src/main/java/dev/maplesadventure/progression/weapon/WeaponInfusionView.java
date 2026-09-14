package dev.maplesadventure.progression.weapon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record WeaponInfusionView(ResourceLocation id,String translationKey,ResourceLocation icon,
                                 WeaponInfusionBuildup futureBuildup,Status status) {
    public enum Status { NORMAL, APPLIED, UNKNOWN, INELIGIBLE }
    public WeaponInfusionView {
        if(id==null||id.toString().length()>256||translationKey==null||translationKey.length()>128||icon!=null&&icon.toString().length()>256)
            throw new IllegalArgumentException("Infusion view bounds");
    }
    public Component displayName() {
        return status==Status.UNKNOWN?Component.translatable("screen.maplesadventure.weapon.infusion_unknown",id.toString())
                :status==Status.INELIGIBLE?Component.translatable("screen.maplesadventure.weapon.infusion_ineligible",id.toString())
                :Component.translatable(translationKey);
    }
    public static WeaponInfusionView normal() {
        var definition=WeaponInfusionRegistry.definitions().get(WeaponInfusionRegistry.NORMAL_ID);
        return of(definition,Status.NORMAL);
    }
    public static WeaponInfusionView of(WeaponInfusionDefinition definition,Status status) {
        return new WeaponInfusionView(definition.id(),definition.translationKey(),definition.icon(),definition.futureBuildup(),status);
    }
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(id.toString(),256); buffer.writeUtf(translationKey,128); buffer.writeBoolean(icon!=null);
        if(icon!=null) buffer.writeUtf(icon.toString(),256); buffer.writeVarInt(futureBuildup.ordinal()); buffer.writeVarInt(status.ordinal());
    }
    public static WeaponInfusionView read(RegistryFriendlyByteBuf buffer) {
        var id=ResourceLocation.parse(buffer.readUtf(256)); var key=buffer.readUtf(128);
        var icon=buffer.readBoolean()?ResourceLocation.parse(buffer.readUtf(256)):null;
        int buildup=buffer.readVarInt(),status=buffer.readVarInt();
        if(buildup<0||buildup>=WeaponInfusionBuildup.values().length||status<0||status>=Status.values().length)
            throw new IllegalArgumentException("Infusion view enum bounds");
        return new WeaponInfusionView(id,key,icon,WeaponInfusionBuildup.values()[buildup],Status.values()[status]);
    }
}
