package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.EncounterConfig;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import dev.maplesadventure.multiplayer.encounter.boss.BossAttemptState;
import dev.maplesadventure.multiplayer.encounter.boss.BossEncounterRuntime;
import dev.maplesadventure.multiplayer.encounter.boss.PhaseBossBarService;
import dev.maplesadventure.multiplayer.encounter.boss.BossAttemptEntityIndex;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityLinkState;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityRegistry;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityRole;
import dev.maplesadventure.multiplayer.encounter.boss.BossIntegrationRegistry;
import dev.maplesadventure.multiplayer.encounter.fog.BossVictoryReturnService;
import dev.maplesadventure.multiplayer.encounter.fog.FogGateSyncService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** Activation and lifecycle coordinator. All calls happen on the Minecraft server thread. */
public final class EncounterManager {
    public static void tick(MinecraftServer server) {
        if (!EncounterConfig.ENABLED.get() || server.getTickCount() % EncounterConfig.ACTIVATION_SCAN_INTERVAL.get() != 0) return;
        EncounterSavedData data = EncounterSavedData.get(server);
        Set<PhaseEncounterKey> visited = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            PhaseId phase = PhaseManager.state(player).phaseId();
            for (EncounterDefinition definition : data.nearby(player.level().dimension(), player.position())) {
                PhaseEncounterKey key = new PhaseEncounterKey(phase, definition.encounterId());
                if (player.distanceToSqr(definition.anchor())
                        > definition.activationRadius() * definition.activationRadius() || !visited.add(key)) continue;
                PhaseEncounterState state = data.state(phase, definition);
                // Boss encounters are traversal-driven. Keeping an unbound boss READY is important for the
                // administrator workflow (create -> add primary -> build/bind gate) and avoids spawning the
                // boss while its gate is still being configured. COMMON encounters remain radius-driven.
                if (definition.type() == EncounterType.BOSS) continue;
                if (state.status() == EncounterStatus.READY) activate(server, data, definition, phase, state);
            }
        }
    }

    public static boolean activate(MinecraftServer server, EncounterSavedData data, EncounterDefinition definition,
                                   PhaseId phase, PhaseEncounterState state) {
        if (state.status() != EncounterStatus.READY || !definition.validForActivation()) {
            if (definition.type() == EncounterType.BOSS && definition.primaryCount() != 1L) {
                MaplesAdventure.LOGGER.warn("Boss encounter {} cannot activate: expected exactly one PRIMARY, found {}",
                        definition.encounterId(), definition.primaryCount());
            }
            return false;
        }
        ServerLevel level = server.getLevel(definition.dimension());
        if (level == null) return false;
        state.setStatus(EncounterStatus.ACTIVE);
        state.setBossStage(1);
        state.clearLiving();
        BossAttemptState attempt = definition.type() == EncounterType.BOSS
                ? BossEncounterRuntime.createAttempt(server, phase, state.generation()) : null;
        state.setBossAttempt(attempt);
        data.changed();
        if (definition.type() == EncounterType.BOSS) {
            for (ServerPlayer member : server.getPlayerList().getPlayers())
                if (PhaseManager.state(member).phaseId().equals(phase))
                    dev.maplesadventure.multiplayer.coop.SummonSignManager.removeOwner(server, member.getUUID());
        }
        for (EncounterSpawnPoint point : definition.spawnPoints()) {
            EncounterSpawnService.spawn(level, definition, point, phase, state.generation(), attempt)
                    .ifPresent(mob -> {
                        state.addLiving(mob.getUUID());
                        if (point.role() == EncounterSpawnRole.BOSS_PRIMARY && attempt != null) {
                            attempt.setPrimaryBossUuid(mob.getUUID());
                        }
                    });
        }
        if (!state.hasLiving() || definition.type() == EncounterType.BOSS && (attempt == null || !attempt.hasPrimary())) {
            discardLoaded(server, phase, definition.encounterId());
            state.setStatus(EncounterStatus.READY);
            state.clearLiving();
            state.clearBossAttempt();
            data.changed();
            return false;
        }
        data.changed();
        if (EncounterConfig.DEBUG.get()) MaplesAdventure.LOGGER.info(
                "[MaplesAdventure/Encounter] activated id={} phase={} generation={} mobs={}",
                definition.encounterId(), phase, state.generation(), state.livingEntities().size());
        return true;
    }

    public static void onMobDeath(Mob mob) {
        var encounter = mob.getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).orElse(null);
        var phase = mob.getExistingData(ModPhaseAttachments.MOB_PHASE).orElse(null);
        if (encounter == null || phase == null || mob.level().isClientSide()) return;
        MinecraftServer server = mob.getServer();
        if (server == null) return;
        EncounterSavedData data = EncounterSavedData.get(server);
        EncounterDefinition definition = data.definition(encounter.encounterId()).orElse(null);
        PhaseEncounterState state = definition == null ? null : data.existingState(phase.phaseId(), encounter.encounterId()).orElse(null);
        if (state == null || state.status() != EncounterStatus.ACTIVE || state.generation() != encounter.generation()) return;
        state.removeLiving(mob.getUUID());
        BossEntityLinkState bossLink = mob.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).orElse(null);
        boolean primary = bossLink != null ? bossLink.role() == BossEntityRole.PRIMARY : encounter.isPrimary();
        if (definition.type() == EncounterType.BOSS && primary) {
            BossIntegrationRegistry.onDefeated(mob);
            UUID attemptId = state.bossAttempt() == null ? null : state.bossAttempt().attemptId();
            state.setStatus(EncounterStatus.DEFEATED);
            PhaseBossBarService.remove(phase.phaseId(), definition.encounterId());
            BossEntityRegistry.discardLoadedAttempt(server, phase.phaseId(), definition.encounterId(), encounter.generation(),
                    mob.getUUID());
            if (attemptId != null) BossAttemptEntityIndex.clearAttempt(attemptId);
            state.clearLiving();
            state.clearBossAttempt();
            FogGateSyncService.syncPhase(server, phase.phaseId());
            BossVictoryReturnService.schedule(server, phase.phaseId());
            if (EncounterConfig.DEBUG.get()) MaplesAdventure.LOGGER.info(
                    "[MaplesAdventure/Encounter] primary defeated id={} phase={}",
                    definition.encounterId(), phase.phaseId());
        } else if (definition.type() == EncounterType.COMMON && !state.hasLiving()) {
            state.setStatus(EncounterStatus.CLEARED);
            if (EncounterConfig.DEBUG.get()) MaplesAdventure.LOGGER.info(
                    "[MaplesAdventure/Encounter] completed id={} phase={} status={}",
                    definition.encounterId(), phase.phaseId(), state.status());
        }
        data.changed();
    }

    /** Returns false for stale encounter entities, which callers should discard without loading any other chunk. */
    public static boolean validateLoadedEntity(Entity entity) {
        var encounter = entity.getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).orElse(null);
        var phase = entity.getExistingData(ModPhaseAttachments.MOB_PHASE).orElse(null);
        if (encounter == null) return true;
        if (phase == null || entity.getServer() == null) return false;
        EncounterSavedData data = EncounterSavedData.get(entity.getServer());
        EncounterDefinition definition = data.definition(encounter.encounterId()).orElse(null);
        PhaseEncounterState state = definition == null ? null : data.existingState(phase.phaseId(), encounter.encounterId()).orElse(null);
        if (state == null || state.status() != EncounterStatus.ACTIVE || state.generation() != encounter.generation()) return false;
        if (definition.type() == EncounterType.BOSS
                && !BossEncounterRuntime.restoreLoadedEntity(entity, definition, state, phase.phaseId())) return false;
        if (definition.type() == EncounterType.BOSS) data.changed(); // persists lazy lineage/legacy migration
        if (!state.livingEntities().contains(entity.getUUID())) {
            state.addLiving(entity.getUUID());
            data.changed();
        }
        return true;
    }

    public static boolean validateLoadedMob(Mob mob) { return validateLoadedEntity(mob); }

    public static int discardLoaded(MinecraftServer server, PhaseId phase, ResourceLocation encounterId) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                var mobPhase = entity.getExistingData(ModPhaseAttachments.MOB_PHASE).orElse(null);
                var encounter = entity.getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).orElse(null);
                var bossLink = entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).orElse(null);
                boolean encounterMobMatch = mobPhase != null && encounter != null && mobPhase.phaseId().equals(phase)
                        && (encounterId == null || encounter.encounterId().equals(encounterId));
                boolean bossLinkMatch = bossLink != null && bossLink.valid() && bossLink.phaseId().equals(phase)
                        && (encounterId == null || bossLink.encounterId().equals(encounterId));
                if (encounterMobMatch || bossLinkMatch) {
                    entity.discard();
                    removed++;
                }
            }
        }
        return removed;
    }

    public static boolean setBossStage(MinecraftServer server, PhaseId phase, ResourceLocation encounterId, int stage) {
        EncounterSavedData data = EncounterSavedData.get(server);
        EncounterDefinition definition = data.definition(encounterId).orElse(null);
        PhaseEncounterState state = definition == null ? null : data.existingState(phase, encounterId).orElse(null);
        if (definition == null || definition.type() != EncounterType.BOSS || state == null
                || state.status() != EncounterStatus.ACTIVE) return false;
        var attempt = state.bossAttempt();
        ServerLevel level = server.getLevel(definition.dimension());
        Entity primary = attempt == null || level == null ? null : level.getEntity(attempt.primaryBossUuid());
        return primary instanceof Mob mob && BossEncounterRuntime.setStage(mob, stage);
    }

    public static boolean hasActiveBossAttempt(MinecraftServer server, PhaseId phase) {
        return BossEncounterRuntime.hasActiveAttempt(server, phase);
    }

    private EncounterManager() {}
}
