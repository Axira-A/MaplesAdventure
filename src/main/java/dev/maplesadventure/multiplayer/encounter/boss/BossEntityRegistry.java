package dev.maplesadventure.multiplayer.encounter.boss;

import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterMobState;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnPoint;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnRole;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import dev.maplesadventure.multiplayer.encounter.PhaseEncounterState;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.mob.MobPhaseState;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** The only writer for boss-attempt lineage and derived-entity phase identity. */
public final class BossEntityRegistry {
    public static boolean registerSpawnedMob(Mob mob, EncounterDefinition definition, EncounterSpawnPoint point,
                                             PhaseId phase, long generation, BossAttemptState attempt) {
        if (definition.type() != EncounterType.BOSS || attempt == null) return false;
        if (point.role() == EncounterSpawnRole.BOSS_PRIMARY)
            return registerPrimary(mob, definition, point, phase, generation, attempt);
        return registerAdd(mob, definition, point, phase, generation, attempt);
    }

    public static boolean registerPrimary(Mob mob, EncounterDefinition definition, EncounterSpawnPoint point,
                                          PhaseId phase, long generation, BossAttemptState attempt) {
        return registerEncounterMob(mob, definition, point, phase, generation, attempt, BossEntityRole.PRIMARY);
    }

    public static boolean registerAdd(Mob mob, EncounterDefinition definition, EncounterSpawnPoint point,
                                      PhaseId phase, long generation, BossAttemptState attempt) {
        BossEntityRole role = point.role() == EncounterSpawnRole.BOSS_CHILD
                ? BossEntityRole.CHILD : BossEntityRole.ADD;
        return registerEncounterMob(mob, definition, point, phase, generation, attempt, role);
    }

    private static boolean registerEncounterMob(Mob mob, EncounterDefinition definition, EncounterSpawnPoint point,
                                                PhaseId phase, long generation, BossAttemptState attempt,
                                                BossEntityRole role) {
        if (definition.type() != EncounterType.BOSS || attempt == null) return false;
        UUID group = UUID.nameUUIDFromBytes((phase + ":" + definition.encounterId() + ":" + generation)
                .getBytes(StandardCharsets.UTF_8));
        mob.setData(ModPhaseAttachments.MOB_PHASE, MobPhaseState.encounter(phase, group));
        mob.setData(ModPhaseAttachments.ENCOUNTER_MOB, new EncounterMobState(definition.encounterId(),
                point.spawnPointId(), generation, true, point.role(), attempt.attemptId(),
                attempt.bossStage(), true));
        UUID primary = role == BossEntityRole.PRIMARY ? mob.getUUID() : attempt.primaryBossUuid();
        mob.setData(ModPhaseAttachments.BOSS_ENTITY_LINK, new BossEntityLinkState(attempt.attemptId(), phase,
                definition.encounterId(), generation, primary, role, true));
        return true;
    }

    /** Copies a validated attempt identity without adding the child to the level. */
    public static boolean registerDerived(Entity parent, Entity child, BossSpawnClassification classification) {
        if (parent == null || child == null || classification == null || child.isAddedToLevel()) return false;
        RuntimeContext context = context(parent);
        if (context == null) return false;
        BossEntityRole role = classification.role();
        BossEntityLinkState link = new BossEntityLinkState(context.attempt.attemptId(), context.phase,
                context.encounter.encounterId(), context.encounter.generation(),
                context.attempt.primaryBossUuid(), role, classification.completionRelevant());
        child.setData(ModPhaseAttachments.BOSS_ENTITY_LINK, link);
        if (child instanceof Mob mob) {
            UUID group = UUID.nameUUIDFromBytes((context.phase + ":" + context.encounter.encounterId() + ":"
                    + context.encounter.generation()).getBytes(StandardCharsets.UTF_8));
            mob.setData(ModPhaseAttachments.MOB_PHASE, MobPhaseState.encounter(context.phase, group));
            EncounterSpawnRole encounterRole = role == BossEntityRole.ADD
                    ? EncounterSpawnRole.BOSS_ADD : EncounterSpawnRole.BOSS_CHILD;
            mob.setData(ModPhaseAttachments.ENCOUNTER_MOB, new EncounterMobState(context.encounter.encounterId(),
                    UUID.randomUUID(), context.encounter.generation(), true, encounterRole,
                    context.attempt.attemptId(), context.attempt.bossStage(), classification.completionRelevant()));
            mob.setPersistenceRequired();
        }
        return true;
    }

    public static boolean registerChild(Entity parent, Entity child, boolean completionRelevant) {
        return registerDerived(parent, child, BossSpawnClassification.child(completionRelevant));
    }
    public static boolean registerProjectile(Entity parent, Entity projectile) {
        return registerDerived(parent, projectile, BossSpawnClassification.projectile());
    }
    public static boolean registerEffect(Entity parent, Entity effect) {
        return registerDerived(parent, effect, BossSpawnClassification.effect());
    }
    public static boolean registerPart(Entity parent, Entity part) {
        return registerDerived(parent, part, BossSpawnClassification.part());
    }

    /** Writes all replacement identity before the new primary can enter the level. */
    public static boolean preparePrimaryReplacement(Mob oldPrimary, Mob newPrimary, int newStage) {
        if (newPrimary.isAddedToLevel()) return false;
        RuntimeContext context = context(oldPrimary);
        if (context == null || context.link == null || context.link.role() != BossEntityRole.PRIMARY
                || !context.attempt.primaryBossUuid().equals(oldPrimary.getUUID())) return false;
        var oldMobPhase = oldPrimary.getExistingData(ModPhaseAttachments.MOB_PHASE).orElse(null);
        if (oldMobPhase == null) return false;
        newPrimary.setData(ModPhaseAttachments.MOB_PHASE, oldMobPhase);
        newPrimary.setData(ModPhaseAttachments.ENCOUNTER_MOB, new EncounterMobState(
                context.encounter.encounterId(), context.encounter.spawnPointId(), context.encounter.generation(),
                true, EncounterSpawnRole.BOSS_PRIMARY, context.attempt.attemptId(), newStage, true));
        newPrimary.setData(ModPhaseAttachments.BOSS_ENTITY_LINK, new BossEntityLinkState(
                context.attempt.attemptId(), context.phase, context.encounter.encounterId(),
                context.encounter.generation(), newPrimary.getUUID(), BossEntityRole.PRIMARY, true));
        newPrimary.setPersistenceRequired();
        return true;
    }

    public static boolean replacePrimary(Mob oldPrimary, Mob newPrimary, int newStage) {
        return BossEncounterRuntime.replacePrimary(oldPrimary, newPrimary, newStage);
    }

    /** Reconciles old boss entities that predate BossEntityLinkState. */
    public static boolean restoreLegacy(Entity entity, EncounterDefinition definition, PhaseEncounterState state,
                                        PhaseId phase) {
        EncounterMobState encounter = entity.getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).orElse(null);
        BossAttemptState attempt = state.bossAttempt();
        if (encounter == null || definition.type() != EncounterType.BOSS || attempt == null) return true;
        EncounterSpawnRole configuredRole = definition.spawnPoints().stream()
                .filter(point -> point.spawnPointId().equals(encounter.spawnPointId()))
                .map(EncounterSpawnPoint::role).findFirst().orElse(encounter.role());
        boolean legacyAttempt = BossAttemptState.NIL_UUID.equals(encounter.attemptId());
        if (!legacyAttempt && !encounter.attemptId().equals(attempt.attemptId())) return false;
        EncounterMobState reconciled = new EncounterMobState(encounter.encounterId(), encounter.spawnPointId(),
                encounter.generation(), true, configuredRole, attempt.attemptId(), attempt.bossStage(),
                encounter.completionRelevant());
        entity.setData(ModPhaseAttachments.ENCOUNTER_MOB, reconciled);
        BossEntityRole role = switch (configuredRole) {
            case BOSS_PRIMARY -> BossEntityRole.PRIMARY;
            case BOSS_ADD, NORMAL -> BossEntityRole.ADD;
            case BOSS_CHILD -> BossEntityRole.CHILD;
        };
        if (role == BossEntityRole.PRIMARY) {
            if (attempt.hasPrimary() && !attempt.primaryBossUuid().equals(entity.getUUID())) return false;
            attempt.setPrimaryBossUuid(entity.getUUID());
            if (entity instanceof Mob mob) BossEncounterRuntime.applyPrimaryScaling(mob, attempt, false);
        }
        entity.setData(ModPhaseAttachments.BOSS_ENTITY_LINK, new BossEntityLinkState(attempt.attemptId(), phase,
                encounter.encounterId(), encounter.generation(), attempt.primaryBossUuid(), role,
                encounter.completionRelevant()));
        return true;
    }

    /** Validates an attached entity against current SavedData and rebuilds the runtime index. */
    public static boolean validateAndTrack(Entity entity) {
        BossEntityLinkState link = entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).orElse(null);
        if (link == null || !link.valid()) return true;
        MinecraftServer server = entity.getServer();
        if (server == null) return false;
        EncounterSavedData data = EncounterSavedData.get(server);
        EncounterDefinition definition = data.definition(link.encounterId()).orElse(null);
        PhaseEncounterState state = definition == null ? null
                : data.existingState(link.phaseId(), link.encounterId()).orElse(null);
        BossAttemptState attempt = state == null ? null : state.bossAttempt();
        if (definition == null || definition.type() != EncounterType.BOSS || state.status() != EncounterStatus.ACTIVE
                || state.generation() != link.generation() || attempt == null
                || !attempt.attemptId().equals(link.attemptId())) return false;
        if (link.role() == BossEntityRole.PRIMARY) {
            if (attempt.hasPrimary() && !attempt.primaryBossUuid().equals(entity.getUUID())) return false;
            attempt.setPrimaryBossUuid(entity.getUUID());
        } else if (!attempt.primaryBossUuid().equals(link.primaryBossUuid())) {
            // A primary replacement changes this diagnostic pointer, not the derivative's attempt identity.
            // Reconcile loaded/unloaded children lazily without force-loading their chunks.
            link = new BossEntityLinkState(link.attemptId(), link.phaseId(), link.encounterId(), link.generation(),
                    attempt.primaryBossUuid(), link.role(), link.completionRelevant());
            entity.setData(ModPhaseAttachments.BOSS_ENTITY_LINK, link);
        }
        BossAttemptEntityIndex.track(entity.getUUID(), link);
        if (link.role().persistentMember() && !state.livingEntities().contains(entity.getUUID())) {
            state.addLiving(entity.getUUID());
            data.changed();
        }
        return true;
    }

    public static void untrack(Entity entity) {
        entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK)
                .ifPresent(link -> BossAttemptEntityIndex.untrack(entity.getUUID(), link));
    }

    public static int discardLoadedAttempt(MinecraftServer server, PhaseId phase, ResourceLocation encounterId,
                                           long generation, UUID except) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (except != null && entity.getUUID().equals(except)) continue;
                BossEntityLinkState link = entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).orElse(null);
                if (link != null && link.valid() && link.phaseId().equals(phase)
                        && link.encounterId().equals(encounterId) && link.generation() == generation) {
                    entity.discard();
                    removed++;
                }
            }
        }
        return removed;
    }

    public static void notifyAttemptReset(MinecraftServer server, PhaseId phase, ResourceLocation encounterId,
                                          long generation) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                BossEntityLinkState link = entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).orElse(null);
                if (entity instanceof Mob mob && link != null && link.role() == BossEntityRole.PRIMARY
                        && link.phaseId().equals(phase) && link.encounterId().equals(encounterId)
                        && link.generation() == generation) {
                    BossIntegrationRegistry.onAttemptReset(mob);
                    return;
                }
            }
        }
    }

    public static boolean belongsToAttempt(Entity entity, UUID attemptId) {
        return entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK)
                .filter(BossEntityLinkState::valid).map(link -> link.attemptId().equals(attemptId)).orElse(false);
    }

    static RuntimeContext context(Entity entity) {
        if (!(entity.level() instanceof ServerLevel) || entity.getServer() == null) return null;
        BossEntityLinkState link = entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK).orElse(null);
        EncounterMobState encounter = entity.getExistingData(ModPhaseAttachments.ENCOUNTER_MOB).orElse(null);
        var mobPhase = entity.getExistingData(ModPhaseAttachments.MOB_PHASE).orElse(null);
        if (link == null && (encounter == null || mobPhase == null || !encounter.boss())) return null;
        PhaseId phase = link != null ? link.phaseId() : mobPhase.phaseId();
        ResourceLocation encounterId = link != null ? link.encounterId() : encounter.encounterId();
        EncounterSavedData data = EncounterSavedData.get(entity.getServer());
        PhaseEncounterState state = data.existingState(phase, encounterId).orElse(null);
        BossAttemptState attempt = state == null ? null : state.bossAttempt();
        long generation = link != null ? link.generation() : encounter.generation();
        UUID attemptId = link != null ? link.attemptId() : encounter.attemptId();
        if (state == null || state.status() != EncounterStatus.ACTIVE || attempt == null
                || state.generation() != generation || !attempt.attemptId().equals(attemptId)) return null;
        EncounterMobState effectiveEncounter = encounter != null ? encounter : new EncounterMobState(encounterId,
                UUID.randomUUID(), generation, true, EncounterSpawnRole.BOSS_CHILD, attemptId,
                attempt.bossStage(), false);
        return new RuntimeContext(data, state, attempt, effectiveEncounter, phase, link);
    }

    record RuntimeContext(EncounterSavedData data, PhaseEncounterState state, BossAttemptState attempt,
                          EncounterMobState encounter, PhaseId phase, BossEntityLinkState link) {}

    private BossEntityRegistry() {}
}
