package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.client.message.MessageClientActions;
import dev.maplesadventure.client.message.MessageRenderCache;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import dev.maplesadventure.interaction.MessageInteractionTarget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MessageInteractionProvider implements InteractionTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "player_message");

    @Override public ResourceLocation id() { return ID; }

    @Override
    public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return target instanceof MessageInteractionTarget messageTarget
                && MessageRenderCache.get(messageTarget.messageId()).isPresent();
    }

    @Override
    public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return supports(level, player, target);
    }

    @Override
    public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return Component.translatable("message.maplesadventure.target_name");
    }

    @Override
    public Component getPrompt(Component keyName, Component displayName, boolean showTargetName) {
        return Component.translatable("hud.maplesadventure.read_message", keyName);
    }

    @Override public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return InteractionPriority.NORMAL;
    }

    @Override
    public boolean interactVirtual(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof MessageInteractionTarget messageTarget)) return false;
        MessageClientActions.requestRead(messageTarget.messageId());
        return true;
    }
}
