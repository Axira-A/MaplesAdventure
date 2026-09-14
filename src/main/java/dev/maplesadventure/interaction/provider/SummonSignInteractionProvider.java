package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import dev.maplesadventure.interaction.SummonSignInteractionTarget;
import dev.maplesadventure.multiplayer.coop.client.SummonClientActions;
import dev.maplesadventure.multiplayer.coop.client.SummonSignRenderCache;
import dev.maplesadventure.multiplayer.coop.SummonSignType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SummonSignInteractionProvider implements InteractionTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "summon_sign");
    @Override public ResourceLocation id() { return ID; }

    @Override
    public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof SummonSignInteractionTarget signTarget)) return false;
        if (dev.maplesadventure.multiplayer.phase.PhaseRelations.state(player).role()
                != dev.maplesadventure.multiplayer.phase.PhaseRole.SOLO) return false;
        return SummonSignRenderCache.get(signTarget.signId())
                .filter(sign -> !sign.ownerUuid().equals(player.getUUID())).isPresent();
    }

    @Override public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return supports(level, player, target);
    }

    @Override public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (target instanceof SummonSignInteractionTarget signTarget
                && SummonSignRenderCache.get(signTarget.signId()).map(sign -> sign.type() == SummonSignType.DUEL).orElse(false))
            return Component.translatable("duel.maplesadventure.target");
        return Component.translatable("summon.maplesadventure.target");
    }

    @Override public Component getPrompt(Component keyName, Component displayName, boolean showTargetName) {
        return Component.translatable("hud.maplesadventure.summon_target", keyName, displayName);
    }

    @Override public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return InteractionPriority.HIGH;
    }

    @Override public boolean interactVirtual(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof SummonSignInteractionTarget signTarget)) return false;
        SummonClientActions.requestSummon(signTarget.signId());
        return true;
    }
}
