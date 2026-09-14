package dev.maplesadventure.multiplayer.encounter.boss;

import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Early server lifecycle bridge for reliable owner-derived lineage and stale-generation rejection. */
public final class BossEntityEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new BossEntityEvents()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Entity entity = event.getEntity();
        if (entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).isEmpty()) {
            BossLineageResolver.parentOf(entity).ifPresent(parent -> {
                if (parent.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).isPresent()) {
                    BossEntityRegistry.registerDerived(parent, entity, BossLineageResolver.classification(entity));
                }
            });
        }
        if (entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).isPresent()
                && !BossEntityRegistry.validateAndTrack(entity)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) BossEntityRegistry.untrack(event.getEntity());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) { BossAttemptEntityIndex.clear(); }

    private BossEntityEvents() {}
}
