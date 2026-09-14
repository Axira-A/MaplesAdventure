package dev.maplesadventure.multiplayer.phase.loot;

import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Constructs provenance from the dying mob and validates generation-bound world objects. */
public final class PhaseLootService {
    public static Optional<PhaseObjectState> stateForEncounterDeath(LivingEntity entity) {
        var phase = entity.getExistingData(ModPhaseAttachments.MOB_PHASE).orElse(null);
        var encounter = entity.getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).orElse(null);
        MinecraftServer server = entity.getServer();
        if (phase == null || encounter == null || server == null) return Optional.empty();
        boolean defeatedBoss = encounter.boss() && encounter.isPrimary()
                && EncounterSavedData.get(server).existingState(phase.phaseId(), encounter.encounterId())
                .filter(state -> state.status() == EncounterStatus.DEFEATED).isPresent();
        return Optional.of(new PhaseObjectState(phase.phaseId(), encounter.encounterId(), encounter.generation(),
                defeatedBoss ? PhaseObjectOrigin.BOSS_REWARD : PhaseObjectOrigin.COMMON_ENCOUNTER));
    }

    public static boolean isCurrent(Entity entity) {
        PhaseObjectState object = entity.getExistingData(ModPhaseAttachments.PHASE_OBJECT).orElse(null);
        if (object == null || object.origin() == PhaseObjectOrigin.BOSS_REWARD) return true;
        MinecraftServer server = entity.getServer();
        if (server == null) return false;
        return EncounterSavedData.get(server).existingState(object.phaseId(), object.encounterId())
                .filter(state -> state.generation() == object.generation()).isPresent();
    }

    /** Scans loaded entities only. It never requests or force-loads a chunk. */
    public static int discardLoadedCommon(MinecraftServer server, PhaseId phase, ResourceLocation encounterId) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                PhaseObjectState state = entity.getExistingData(ModPhaseAttachments.PHASE_OBJECT).orElse(null);
                if (state != null && state.origin() == PhaseObjectOrigin.COMMON_ENCOUNTER
                        && state.phaseId().equals(phase)
                        && (encounterId == null || state.encounterId().equals(encounterId))) {
                    entity.discard();
                    removed++;
                }
            }
        }
        return removed;
    }

    private PhaseLootService() {}
}
