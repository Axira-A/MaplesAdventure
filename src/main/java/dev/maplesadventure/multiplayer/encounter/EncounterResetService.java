package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.EncounterConfig;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.multiplayer.phase.loot.PhaseLootService;
import dev.maplesadventure.multiplayer.encounter.boss.PhaseBossBarService;
import dev.maplesadventure.multiplayer.encounter.boss.BossAttemptEntityIndex;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityRegistry;
import dev.maplesadventure.multiplayer.encounter.fog.FogTraversalManager;
import dev.maplesadventure.multiplayer.encounter.fog.FogGateSyncService;

public final class EncounterResetService {
    public static boolean resetForPlayer(ServerPlayer player, EncounterResetReason reason) {
        PhaseRole role = PhaseManager.state(player).role();
        if (role != PhaseRole.SOLO && role != PhaseRole.HOST) return false;
        return resetPhase(player.server, PhaseManager.state(player).phaseId(), reason);
    }

    public static boolean resetPhase(MinecraftServer server, PhaseId phase, EncounterResetReason reason) {
        EncounterSavedData data = EncounterSavedData.get(server);
        long generation = data.incrementGeneration(phase);
        for (var entry : data.runtimeSnapshot().entrySet()) {
            if (!entry.getKey().phaseId().equals(phase)) continue;
            EncounterDefinition definition = data.definition(entry.getKey().encounterId()).orElse(null);
            if (definition == null) continue;
            PhaseEncounterState state = entry.getValue();
            if (definition.type() == EncounterType.BOSS && state.status() == EncounterStatus.DEFEATED
                    && !EncounterConfig.RESPAWN_DEFEATED_BOSSES.get()) continue;
            var oldAttempt = state.bossAttempt();
            long oldGeneration = state.generation();
            if (definition.type() == EncounterType.BOSS) {
                BossEntityRegistry.notifyAttemptReset(server, phase, definition.encounterId(), oldGeneration);
                if (oldAttempt != null) BossAttemptEntityIndex.clearAttempt(oldAttempt.attemptId());
            }
            state.setStatus(EncounterStatus.READY);
            state.setGeneration(generation);
            state.setBossStage(1);
            state.clearBossAttempt();
            state.clearLiving();
            if (definition.type() == EncounterType.BOSS) PhaseBossBarService.remove(phase, definition.encounterId());
        }
        int removed = EncounterManager.discardLoaded(server, phase, null);
        int removedObjects = PhaseLootService.discardLoadedCommon(server, phase, null);
        FogTraversalManager.clearPhase(server, phase);
        FogGateSyncService.syncPhase(server, phase);
        data.changed();
        if (EncounterConfig.DEBUG.get()) MaplesAdventure.LOGGER.info(
                "[MaplesAdventure/Encounter] reset phase={} generation={} reason={} removedLoaded={}",
                phase, generation, reason, removed + removedObjects);
        return true;
    }

    public static boolean resetEncounter(MinecraftServer server, PhaseId phase, ResourceLocation encounterId,
                                         boolean forceDefeatedBoss) {
        EncounterSavedData data = EncounterSavedData.get(server);
        EncounterDefinition definition = data.definition(encounterId).orElse(null);
        if (definition == null) return false;
        PhaseEncounterState state = data.state(phase, definition);
        if (state.status() == EncounterStatus.DEFEATED && !forceDefeatedBoss
                && !EncounterConfig.RESPAWN_DEFEATED_BOSSES.get()) return false;
        var oldAttempt = state.bossAttempt();
        long oldGeneration = state.generation();
        if (definition.type() == EncounterType.BOSS) {
            BossEntityRegistry.notifyAttemptReset(server, phase, encounterId, oldGeneration);
            if (oldAttempt != null) BossAttemptEntityIndex.clearAttempt(oldAttempt.attemptId());
        }
        state.setStatus(EncounterStatus.READY);
        state.setGeneration(state.generation() == Long.MAX_VALUE ? 1L : state.generation() + 1L);
        state.setBossStage(1);
        state.clearBossAttempt();
        state.clearLiving();
        if (definition.type() == EncounterType.BOSS) PhaseBossBarService.remove(phase, encounterId);
        EncounterManager.discardLoaded(server, phase, encounterId);
        PhaseLootService.discardLoadedCommon(server, phase, encounterId);
        FogTraversalManager.clearPhase(server, phase);
        FogGateSyncService.syncPhase(server, phase);
        data.changed();
        return true;
    }

    private EncounterResetService() {}
}
