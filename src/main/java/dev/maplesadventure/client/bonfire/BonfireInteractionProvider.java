package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.bonfire.BonfireBlockEntity;
import dev.maplesadventure.interaction.BlockInteractionTarget;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import dev.maplesadventure.registry.ModBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** F uses the existing validated block-use pipeline; right-click calls the same block method. */
public final class BonfireInteractionProvider implements InteractionTargetProvider {
    @Override public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "bonfire"); }
    @Override public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return target instanceof BlockInteractionTarget block && block.expectedBlock() == ModBlocks.BONFIRE.get();
    }
    @Override public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return target instanceof BlockInteractionTarget block && level.isLoaded(block.pos())
                && level.getBlockState(block.pos()).is(ModBlocks.BONFIRE.get());
    }
    @Override public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (target instanceof BlockInteractionTarget block && level.getBlockEntity(block.pos()) instanceof BonfireBlockEntity entity
                && !entity.displayName().isBlank()) return Component.literal(entity.displayName());
        return Component.translatable("block.maplesadventure.bonfire");
    }
    @Override public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return InteractionPriority.HIGH;
    }
    @Override public double getMaximumInteractionDistance(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return 1.25D;
    }
}
