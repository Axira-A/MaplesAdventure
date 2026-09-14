package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.interaction.BlockInteractionTarget;
import dev.maplesadventure.interaction.InteractionHitResolver;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public interface InteractionTargetProvider {
    ResourceLocation id();

    boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target);

    boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target);

    Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target);

    default Component getPrompt(Component keyName, Component displayName, boolean showTargetName) {
        return showTargetName
                ? Component.translatable("hud.maplesadventure.interact_target", keyName, displayName)
                : Component.translatable("hud.maplesadventure.interact", keyName);
    }

    default InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return InteractionPriority.NORMAL;
    }

    default Vec3 getMarkerPosition(ClientLevel level, InteractionTarget target, float partialTick) {
        return target.markerPosition(level, partialTick);
    }

    default InteractionHitResolver.BlockHitResolution resolveHitResult(
            ClientLevel level,
            LocalPlayer player,
            BlockInteractionTarget target
    ) {
        return InteractionHitResolver.resolveBlockHit(level, player, target.pos());
    }

    default EntityInteractionMode getEntityInteractionMode(
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target
    ) {
        return EntityInteractionMode.NORMAL;
    }

    /** Allows a server-authored entity to impose a stricter limit than the general client interaction setting. */
    default double getMaximumInteractionDistance(
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target
    ) {
        return InteractionConfig.MAX_INTERACTION_DISTANCE.get();
    }

    /** Executes non-entity/non-block targets without inventing a second F-key pipeline. */
    default boolean interactVirtual(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return false;
    }
}
