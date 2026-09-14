package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.interaction.EntityInteractionTarget;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import dev.maplesadventure.soul.LostSoulEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Exposes only the local player's own soul to the contextual F/Y target list. */
public final class LostSoulInteractionProvider implements InteractionTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "lost_soul");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        LostSoulEntity soul = resolve(level, target);
        return soul != null && player.getUUID().equals(soul.getOwnerUuid());
    }

    @Override
    public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        LostSoulEntity soul = resolve(level, target);
        return soul != null && !soul.isRemoved() && soul.isAlive()
                && player.getUUID().equals(soul.getOwnerUuid());
    }

    @Override
    public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        LostSoulEntity soul = resolve(level, target);
        return soul == null ? Component.empty() : soul.getDisplayName();
    }

    @Override
    public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return InteractionPriority.HIGH;
    }

    @Override
    public EntityInteractionMode getEntityInteractionMode(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        return EntityInteractionMode.GENERAL_ONLY;
    }

    @Override
    public double getMaximumInteractionDistance(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        LostSoulEntity soul = resolve(level, target);
        return soul == null ? 0.0D : soul.getRecoveryDistance();
    }

    private static @Nullable LostSoulEntity resolve(ClientLevel level, InteractionTarget target) {
        if (!(target instanceof EntityInteractionTarget entityTarget)) {
            return null;
        }
        Entity entity = entityTarget.resolve(level);
        return entity instanceof LostSoulEntity soul ? soul : null;
    }
}
