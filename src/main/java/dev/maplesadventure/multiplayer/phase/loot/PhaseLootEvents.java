package dev.maplesadventure.multiplayer.phase.loot;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.entity.XpOrbTargetingEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/** Public-event boundary for final loot tagging, collection security and XP attraction. */
public final class PhaseLootEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new PhaseLootEvents()); }

    /** LOWEST observes the final ItemEntity collection after Vanilla, Looting and ordinary mod handlers. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEncounterDrops(LivingDropsEvent event) {
        PhaseLootService.stateForEncounterDeath(event.getEntity()).ifPresent(state -> {
            for (ItemEntity drop : event.getDrops()) {
                drop.setData(ModPhaseAttachments.PHASE_OBJECT, state.copy());
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!PhaseRelations.canCollect(event.getPlayer(), event.getItemEntity())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        if (!PhaseRelations.canCollect(event.getEntity(), event.getOrb())) event.setCanceled(true);
    }

    /** Replaces only the candidate choice; Vanilla still owns movement, delay and pickup semantics. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onExperienceTarget(XpOrbTargetingEvent event) {
        ExperienceOrb orb = event.getXpOrb();
        if (orb.getExistingData(ModPhaseAttachments.PHASE_OBJECT).isEmpty()) return;
        Player nearest = orb.level().getNearestPlayer(orb.getX(), orb.getY(), orb.getZ(), event.getScanDistance(),
                candidate -> candidate instanceof Player player
                        && EntitySelector.NO_SPECTATORS.test(player)
                        && PhaseRelations.canCollect(player, orb));
        event.setFollowingPlayer(nearest);
    }

    /** Rejects stale COMMON resources as their chunk reloads, without loading any other chunk. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)
                || event.getEntity().getExistingData(ModPhaseAttachments.PHASE_OBJECT).isEmpty()) return;
        if (!PhaseLootService.isCurrent(event.getEntity())) event.setCanceled(true);
    }

    private PhaseLootEvents() {}
}
