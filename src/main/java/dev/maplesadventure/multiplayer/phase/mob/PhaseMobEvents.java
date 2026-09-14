package dev.maplesadventure.multiplayer.phase.mob;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.PhaseConfig;
import dev.maplesadventure.multiplayer.phase.PhaseMembership;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Prototype-only lifecycle rules; natural/shared mobs never receive the attachment. */
public final class PhaseMobEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new PhaseMobEvents()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTargetChange(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && !PhaseRelations.canTarget(event.getEntity(), target)) {
            event.setNewAboutToBeSetTarget(null);
            if (PhaseConfig.DEBUG.get()) {
                MaplesAdventure.LOGGER.info("[MaplesAdventure/Phase] rejected AI target mob={} target={}",
                        event.getEntity().getUUID(), target.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)
                || mob.level().isClientSide()
                || mob.getExistingData(ModPhaseAttachments.MOB_PHASE).isEmpty()) return;
        LivingEntity target = mob.getTarget();
        if (target != null && !PhaseRelations.canTarget(mob, target)) mob.setTarget(null);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob
                && PhaseMembership.isPrototypePhasedMob(mob)) {
            mob.setPersistenceRequired();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrops(LivingDropsEvent event) {
        if (PhaseMembership.isPrototypePhasedMob(event.getEntity())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExperience(LivingExperienceDropEvent event) {
        if (PhaseMembership.isPrototypePhasedMob(event.getEntity())) {
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    private PhaseMobEvents() {}
}
