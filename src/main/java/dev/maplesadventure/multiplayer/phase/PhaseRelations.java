package dev.maplesadventure.multiplayer.phase;

import dev.maplesadventure.multiplayer.phase.client.ClientPhaseState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionManager;

/** The single policy boundary consumed by events and the few required low-level hooks. */
public final class PhaseRelations {
    public static boolean canSee(Entity viewer, Entity target) {
        if (!compatibleMembership(viewer, target)) return false;
        if (target.getExistingData(ModPhaseAttachments.PHASE_OBJECT).isPresent()
                && viewer instanceof Player player && state(player).role() == PhaseRole.INVADER) return false;
        return true;
    }
    public static boolean canInteract(Entity actor, Entity target) {
        if (!compatibleMembership(actor, target)) return false;
        if (opposingPlayers(actor, target)) return false;
        return true;
    }
    public static boolean canCollide(Entity first, Entity second) {
        return compatibleMembership(first, second) && !cooperativePair(first, second)
                && !opposingPlayers(first, second) && !invaderMobPair(first, second);
    }
    public static boolean canTarget(Entity actor, Entity target) {
        Entity responsible = responsibleEntity(actor);
        if (!compatibleMembership(responsible, target) || cooperativePair(responsible, target)
                || invaderMobPair(responsible, target)) return false;
        if (opposingPlayers(responsible, target)) return invasionCombatActive(responsible, target);
        return true;
    }

    public static boolean canDamage(Entity attacker, Entity target) {
        Entity responsible = responsibleEntity(attacker);
        if (!compatibleMembership(responsible, target) || cooperativePair(responsible, target)
                || invaderMobPair(responsible, target)) return false;
        if (opposingPlayers(responsible, target)) return invasionCombatActive(responsible, target);
        return true;
    }

    /**
     * Shared objects are public. A phase-bound object may only be acquired by an entity that
     * explicitly belongs to the same phase; shared mobs and automation must not launder it back
     * into the shared world.
     */
    public static boolean canCollect(Entity collector, Entity object) {
        var objectPhase = PhaseMembership.phaseOf(object);
        if (objectPhase.isEmpty()) return true;
        var collectorPhase = PhaseMembership.phaseOf(collector);
        if (collectorPhase.isEmpty() || !collectorPhase.get().equals(objectPhase.get())) return false;
        return !(collector instanceof Player player) || state(player).role() != PhaseRole.INVADER;
    }

    /** Merge requires an identical access and lifecycle domain, unlike ordinary visibility compatibility. */
    public static boolean canMerge(Entity first, Entity second) {
        var firstObject = first.getExistingData(ModPhaseAttachments.PHASE_OBJECT);
        var secondObject = second.getExistingData(ModPhaseAttachments.PHASE_OBJECT);
        if (firstObject.isPresent() || secondObject.isPresent()) {
            return firstObject.isPresent() && secondObject.isPresent()
                    && firstObject.get().canMergeWith(secondObject.get());
        }
        var firstPhase = PhaseMembership.phaseOf(first);
        var secondPhase = PhaseMembership.phaseOf(second);
        return firstPhase.isEmpty() && secondPhase.isEmpty()
                || firstPhase.isPresent() && secondPhase.isPresent() && firstPhase.get().equals(secondPhase.get());
    }

    /** Hoppers and other unowned Vanilla automation are shared, so they cannot consume phased resources. */
    public static boolean canSharedAutomationCollect(Entity object) {
        return PhaseMembership.phaseOf(object).isEmpty();
    }

    public static boolean samePhase(Player first, Player second) {
        return samePhase((Entity) first, second);
    }

    public static boolean samePhase(Entity first, Entity second) {
        var firstPhase = PhaseMembership.phaseOf(first);
        var secondPhase = PhaseMembership.phaseOf(second);
        return firstPhase.isPresent() && secondPhase.isPresent() && firstPhase.get().equals(secondPhase.get());
    }

    public static PlayerPhaseState state(Player player) {
        if (player.level().isClientSide()) return ClientPhaseState.state(player.getUUID());
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) return PhaseManager.state(serverPlayer);
        return PlayerPhaseState.solo(player.getUUID());
    }

    private static boolean compatibleMembership(Entity first, Entity second) {
        var firstPhase = PhaseMembership.phaseOf(first);
        var secondPhase = PhaseMembership.phaseOf(second);
        return firstPhase.isEmpty() || secondPhase.isEmpty() || firstPhase.get().equals(secondPhase.get());
    }

    private static Entity responsibleEntity(Entity entity) {
        Entity current = entity;
        for (int depth = 0; depth < 8 && current instanceof Projectile projectile
                && projectile.getOwner() != null && projectile.getOwner() != current; depth++) {
            current = projectile.getOwner();
        }
        return current;
    }

    private static boolean cooperativePair(Entity first, Entity second) {
        if (!(first instanceof Player firstPlayer) || !(second instanceof Player secondPlayer)
                || !samePhase(firstPlayer, secondPlayer)) return false;
        PhaseRole firstRole = state(firstPlayer).role();
        PhaseRole secondRole = state(secondPlayer).role();
        boolean rolePair = firstRole == PhaseRole.HOST && secondRole == PhaseRole.COOPERATOR
                || firstRole == PhaseRole.COOPERATOR && secondRole == PhaseRole.HOST;
        if (!rolePair) return false;
        if (first.level().isClientSide()) return true;
        return dev.maplesadventure.multiplayer.coop.CoopSessionManager.arePartners(
                first.getUUID(), second.getUUID());
    }

    private static boolean opposingPlayers(Entity first, Entity second) {
        if (!(first instanceof Player firstPlayer) || !(second instanceof Player secondPlayer)
                || !samePhase(firstPlayer, secondPlayer)) return false;
        PhaseRole firstRole = state(firstPlayer).role();
        PhaseRole secondRole = state(secondPlayer).role();
        boolean rolePair = firstRole == PhaseRole.INVADER && (secondRole == PhaseRole.HOST || secondRole == PhaseRole.COOPERATOR)
                || secondRole == PhaseRole.INVADER && (firstRole == PhaseRole.HOST || firstRole == PhaseRole.COOPERATOR);
        if (!rolePair) return false;
        if (first.level().isClientSide()) return true;
        return InvasionSessionManager.areOpponents(first.getUUID(), second.getUUID());
    }

    private static boolean invasionCombatActive(Entity first, Entity second) {
        if (first.level().isClientSide()) return true;
        return InvasionSessionManager.isCombatActive(first.getUUID())
                && InvasionSessionManager.isCombatActive(second.getUUID());
    }

    private static boolean invaderMobPair(Entity first, Entity second) {
        Entity responsibleFirst = responsibleEntity(first);
        Entity responsibleSecond = responsibleEntity(second);
        boolean firstInvader = responsibleFirst instanceof Player player && state(player).role() == PhaseRole.INVADER;
        boolean secondInvader = responsibleSecond instanceof Player player && state(player).role() == PhaseRole.INVADER;
        boolean firstMob = !(responsibleFirst instanceof Player) && PhaseMembership.phaseOf(responsibleFirst).isPresent();
        boolean secondMob = !(responsibleSecond instanceof Player) && PhaseMembership.phaseOf(responsibleSecond).isPresent();
        return firstInvader && secondMob || secondInvader && firstMob;
    }

    private PhaseRelations() {}
}
