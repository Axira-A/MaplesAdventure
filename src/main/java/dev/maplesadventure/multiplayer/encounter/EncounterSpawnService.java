package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.mob.MobPhaseState;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import dev.maplesadventure.multiplayer.encounter.boss.BossAttemptState;
import dev.maplesadventure.multiplayer.encounter.boss.BossEncounterRuntime;
import dev.maplesadventure.multiplayer.encounter.boss.BossEntityRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

public final class EncounterSpawnService {
    public static Optional<Mob> spawn(ServerLevel level, EncounterDefinition definition,
                                      EncounterSpawnPoint point, PhaseId phaseId, long generation,
                                      BossAttemptState bossAttempt) {
        BlockPos pos = BlockPos.containing(point.position());
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) return Optional.empty();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(point.entityType()).orElse(null);
        if (type == null) return Optional.empty();
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) created.discard();
            MaplesAdventure.LOGGER.warn("Encounter {} spawn point {} is not a Mob: {}",
                    definition.encounterId(), point.spawnPointId(), point.entityType());
            return Optional.empty();
        }
        mob.moveTo(point.position().x, point.position().y, point.position().z, point.yaw(), 0.0F);
        if (!level.noBlockCollision(mob, mob.getBoundingBox()) || !level.getFluidState(pos).isEmpty()) {
            mob.discard();
            return Optional.empty();
        }
        if (definition.type() == EncounterType.BOSS) {
            if (!BossEntityRegistry.registerSpawnedMob(mob, definition, point, phaseId, generation, bossAttempt)) {
                mob.discard();
                return Optional.empty();
            }
        } else {
            UUID group = UUID.nameUUIDFromBytes((phaseId + ":" + definition.encounterId() + ":" + generation)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            mob.setData(ModPhaseAttachments.MOB_PHASE, MobPhaseState.encounter(phaseId, group));
            mob.setData(ModPhaseAttachments.ENCOUNTER_MOB, new EncounterMobState(definition.encounterId(),
                    point.spawnPointId(), generation, false, point.role(), BossAttemptState.NIL_UUID, 1, true));
        }
        mob.setPersistenceRequired();
        try (EncounterSpawnContext ignored = EncounterSpawnContext.enter()) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            if (point.role() == EncounterSpawnRole.BOSS_PRIMARY && bossAttempt != null) {
                BossEncounterRuntime.applyPrimaryScaling(mob, bossAttempt, true);
            }
            boolean added = level.addFreshEntity(mob);
            if (added && point.role() == EncounterSpawnRole.BOSS_PRIMARY) {
                dev.maplesadventure.multiplayer.encounter.boss.BossIntegrationRegistry.onAttemptStarted(mob);
            }
            return added ? Optional.of(mob) : Optional.empty();
        }
    }

    private EncounterSpawnService() {}
}
