package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.config.EncounterConfig;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import dev.maplesadventure.multiplayer.encounter.boss.PhaseBossBarService;
import dev.maplesadventure.multiplayer.encounter.fog.BossVictoryReturnService;
import dev.maplesadventure.multiplayer.encounter.fog.FogGateSyncService;
import dev.maplesadventure.multiplayer.encounter.fog.FogTraversalManager;

public final class EncounterEvents {
    private static final Set<MobSpawnType> BLOCKED_AUTOMATIC_TYPES = EnumSet.of(
            MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.PATROL,
            MobSpawnType.REINFORCEMENT, MobSpawnType.STRUCTURE,
            MobSpawnType.SPAWNER, MobSpawnType.TRIAL_SPAWNER);
    private static final Set<UUID> RESPAWN_GUARD = new java.util.HashSet<>();

    public static void register() { NeoForge.EVENT_BUS.register(new EncounterEvents()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (EncounterSpawnContext.active() || !EncounterConfig.MANAGED_SPAWNING.get()
                || event.getEntity().getType().getCategory() != MobCategory.MONSTER
                || !BLOCKED_AUTOMATIC_TYPES.contains(event.getSpawnType())) return;
        ResourceLocation dimension = event.getLevel().getLevel().dimension().location();
        if (EncounterConfig.MANAGED_DIMENSIONS.get().contains(dimension.toString())) event.setSpawnCancelled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || event.getEntity().getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).isEmpty()) return;
        if (!EncounterManager.validateLoadedEntity(event.getEntity())) event.setCanceled(true);
        else if (event.getEntity() instanceof Mob mob) mob.setPersistenceRequired();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onMobDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Mob mob) EncounterManager.onMobDeath(mob);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isEndConquered()) return;
        if (!RESPAWN_GUARD.add(player.getUUID())) return;
        player.server.execute(() -> {
            RESPAWN_GUARD.remove(player.getUUID());
            EncounterResetService.resetForPlayer(player, EncounterResetReason.PLAYER_RESPAWN);
        });
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        EncounterManager.tick(event.getServer());
        PhaseBossBarService.tick(event.getServer());
        EncounterDebugService.tick(event.getServer());
        FogTraversalManager.tick(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) FogGateSyncService.tick(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EncounterDebugService.forget(event.getEntity().getUUID());
        FogTraversalManager.forget(event.getEntity().getUUID());
        FogGateSyncService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        RESPAWN_GUARD.clear();
        EncounterDebugService.clear();
        PhaseBossBarService.clear();
        FogTraversalManager.clear();
        FogGateSyncService.clear();
        BossVictoryReturnService.clear();
    }

    private EncounterEvents() {}
}
