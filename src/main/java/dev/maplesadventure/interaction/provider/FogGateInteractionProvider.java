package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.interaction.FogGateInteractionTarget;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import dev.maplesadventure.multiplayer.encounter.fog.client.FogGateClientCache;
import dev.maplesadventure.multiplayer.encounter.fog.network.FogGatePayloads;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FogGateInteractionProvider implements InteractionTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "boss_fog_gate");
    @Override public ResourceLocation id() { return ID; }
    @Override public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return target instanceof FogGateInteractionTarget gate
                && FogGateClientCache.get(gate.gateId()).map(view -> view.interactable()).orElse(false);
    }
    @Override public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return supports(level, player, target);
    }
    @Override public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return Component.translatable("block.maplesadventure.boss_fog_gate");
    }
    @Override public Component getPrompt(Component keyName, Component displayName, boolean showTargetName) {
        return Component.translatable("hud.maplesadventure.enter_fog", keyName);
    }
    @Override public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return InteractionPriority.HIGH;
    }
    @Override public boolean interactVirtual(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof FogGateInteractionTarget gate) || !supports(level, player, target)) return false;
        PacketDistributor.sendToServer(new FogGatePayloads.RequestEntry(gate.gateId()));
        return true;
    }
}
