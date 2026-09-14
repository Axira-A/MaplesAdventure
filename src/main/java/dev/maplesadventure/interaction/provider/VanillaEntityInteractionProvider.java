package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.interaction.EntityInteractionTarget;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

public final class VanillaEntityInteractionProvider implements InteractionTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "vanilla_entity");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof EntityInteractionTarget entityTarget)) {
            return false;
        }
        Entity entity = entityTarget.resolve(level);
        return entity instanceof AbstractVillager
                || entity instanceof AbstractHorse
                || entity instanceof Boat
                || entity instanceof AbstractMinecart
                || entity instanceof TamableAnimal animal && (
                        animal.isTame()
                                || animal.isFood(player.getMainHandItem())
                                || animal.isFood(player.getOffhandItem())
                )
                || entity instanceof PlayerRideable
                || entity instanceof MenuProvider;
    }

    @Override
    public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof EntityInteractionTarget entityTarget)) {
            return false;
        }
        Entity entity = entityTarget.resolve(level);
        return entity != null && entity != player && !entity.isRemoved() && entity.isAlive();
    }

    @Override
    public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        Entity entity = ((EntityInteractionTarget) target).resolve(level);
        return entity == null ? Component.empty() : entity.getDisplayName();
    }

    @Override
    public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        Entity entity = ((EntityInteractionTarget) target).resolve(level);
        return entity instanceof AbstractVillager ? InteractionPriority.HIGH : InteractionPriority.NORMAL;
    }
}
