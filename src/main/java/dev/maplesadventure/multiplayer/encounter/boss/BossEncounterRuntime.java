package dev.maplesadventure.multiplayer.encounter.boss;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.EncounterConfig;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterMobState;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnContext;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnRole;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import dev.maplesadventure.multiplayer.encounter.PhaseEncounterState;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Authoritative primary/stage/scaling operations for one boss attempt. */
public final class BossEncounterRuntime {
    public static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "boss_coop_health");

    public static BossAttemptState createAttempt(MinecraftServer server, PhaseId phase, long generation) {
        int partySize = CoopSessionManager.formalPartySize(server, phase);
        double multiplier = partySize >= 2 ? EncounterConfig.BOSS_HEALTH_MULTIPLIER_COOP.get()
                : EncounterConfig.BOSS_HEALTH_MULTIPLIER_SOLO.get();
        return BossAttemptState.create(generation, partySize, multiplier);
    }

    public static void applyPrimaryScaling(Mob boss, BossAttemptState attempt, boolean refill) {
        AttributeInstance maxHealth = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        if (attempt.healthMultiplier() != 1.0D) {
            maxHealth.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_MODIFIER_ID,
                    attempt.healthMultiplier() - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (refill) boss.setHealth(boss.getMaxHealth());
        else boss.setHealth(Math.min(boss.getHealth(), boss.getMaxHealth()));
    }

    /** Reconciles legacy attachments and restores scaling/primary identity after chunk or server load. */
    public static boolean restoreLoadedEntity(Entity entity, EncounterDefinition definition,
                                              PhaseEncounterState state, PhaseId phase) {
        if (!BossEntityRegistry.restoreLegacy(entity, definition, state, phase)) return false;
        return BossEntityRegistry.validateAndTrack(entity);
    }

    public static boolean setStage(Mob boss, int stage) {
        RuntimeContext context = context(boss);
        if (context == null || !context.encounter.isPrimary()
                || !context.attempt.primaryBossUuid().equals(boss.getUUID())) return false;
        context.attempt.setBossStage(stage);
        context.state.setBossStage(stage);
        boss.setData(ModPhaseAttachments.ENCOUNTER_MOB,
                context.encounter.withRoleAndAttempt(EncounterSpawnRole.BOSS_PRIMARY,
                        context.attempt.attemptId(), stage));
        context.data.changed();
        BossIntegrationRegistry.onStageChanged(boss, stage);
        return true;
    }

    /** New boss must not yet be in the level, preventing a temporary SHARED entity window. */
    public static boolean replacePrimary(Mob oldBoss, Mob newBoss, int newStage) {
        RuntimeContext context = context(oldBoss);
        if (context == null || !context.encounter.isPrimary() || newBoss.isAddedToLevel()
                || !context.attempt.primaryBossUuid().equals(oldBoss.getUUID())) return false;
        ServerLevel level = (ServerLevel) oldBoss.level();
        if (!BossEntityRegistry.preparePrimaryReplacement(oldBoss, newBoss, newStage)) return false;
        applyPrimaryScaling(newBoss, context.attempt, true);
        UUID oldPrimary = context.attempt.primaryBossUuid();
        int oldStage = context.attempt.bossStage();
        context.attempt.setPrimaryBossUuid(newBoss.getUUID());
        context.attempt.setBossStage(newStage);
        context.state.setBossStage(newStage);
        context.state.addLiving(newBoss.getUUID());
        boolean added;
        try (EncounterSpawnContext ignored = EncounterSpawnContext.enter()) {
            added = level.addFreshEntity(newBoss);
        }
        if (!added) {
            BossEntityRegistry.untrack(newBoss);
            context.state.removeLiving(newBoss.getUUID());
            context.attempt.setPrimaryBossUuid(oldPrimary);
            context.attempt.setBossStage(oldStage);
            context.state.setBossStage(oldStage);
            return false;
        }
        context.state.removeLiving(oldBoss.getUUID());
        context.data.changed();
        BossIntegrationRegistry.onReplacement(oldBoss, newBoss, newStage);
        oldBoss.discard();
        return true;
    }

    /** Explicit adapter API; the child is attached before it is added and never exists as SHARED. */
    public static boolean attachChild(Mob parentBoss, Entity child, boolean completionRelevant) {
        RuntimeContext context = context(parentBoss);
        if (context == null || !context.encounter.isPrimary() || child.isAddedToLevel()) return false;
        if (!BossEntityRegistry.registerChild(parentBoss, child, completionRelevant)) return false;
        boolean added;
        try (EncounterSpawnContext ignored = EncounterSpawnContext.enter()) {
            added = ((ServerLevel) parentBoss.level()).addFreshEntity(child);
        }
        if (added) {
            context.state.addLiving(child.getUUID());
            context.data.changed();
        }
        return added;
    }

    public static boolean attachProjectile(Mob parentBoss, Entity projectile) {
        return addDerived(parentBoss, projectile, BossSpawnClassification.projectile());
    }

    public static boolean attachEffect(Mob parentBoss, Entity effect) {
        return addDerived(parentBoss, effect, BossSpawnClassification.effect());
    }

    private static boolean addDerived(Mob parentBoss, Entity child, BossSpawnClassification classification) {
        RuntimeContext context = context(parentBoss);
        if (context == null || child.isAddedToLevel()
                || !BossEntityRegistry.registerDerived(parentBoss, child, classification)) return false;
        boolean added;
        try (EncounterSpawnContext ignored = EncounterSpawnContext.enter()) {
            added = ((ServerLevel) parentBoss.level()).addFreshEntity(child);
        }
        if (added && classification.role().persistentMember()) {
            context.state.addLiving(child.getUUID());
            context.data.changed();
        }
        return added;
    }

    public static boolean hasActiveAttempt(MinecraftServer server, PhaseId phase) {
        EncounterSavedData data = EncounterSavedData.get(server);
        return data.runtimeSnapshot().entrySet().stream().anyMatch(entry -> entry.getKey().phaseId().equals(phase)
                && entry.getValue().status() == EncounterStatus.ACTIVE
                && entry.getValue().bossAttempt() != null
                && data.definition(entry.getKey().encounterId()).map(d -> d.type() == EncounterType.BOSS).orElse(false));
    }

    private static RuntimeContext context(Mob boss) {
        var context = BossEntityRegistry.context(boss);
        return context == null ? null : new RuntimeContext(context.data(), context.state(), context.attempt(),
                context.encounter());
    }

    private record RuntimeContext(EncounterSavedData data, PhaseEncounterState state,
                                  BossAttemptState attempt, EncounterMobState encounter) {}
    private BossEncounterRuntime() {}
}
