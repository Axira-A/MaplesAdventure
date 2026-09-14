package dev.maplesadventure.multiplayer.phase;

import dev.maplesadventure.multiplayer.phase.client.ClientPhaseState;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.entity.PartEntity;

/** The only source of Entity-to-Phase identity. Missing membership means SHARED. */
public final class PhaseMembership {
    public static Optional<PhaseId> phaseOf(Entity entity) {
        return phaseOf(entity, 0);
    }

    public static boolean isPrototypePhasedMob(Entity entity) {
        return entity != null && entity.getExistingData(ModPhaseAttachments.MOB_PHASE)
                .filter(state -> state.prototype()).isPresent();
    }

    private static PhaseId playerPhase(Player player) {
        if (player.level().isClientSide()) return ClientPhaseState.state(player.getUUID()).phaseId();
        if (player instanceof ServerPlayer serverPlayer) return PhaseManager.state(serverPlayer).phaseId();
        return PhaseId.solo(player.getUUID());
    }

    private static Optional<PhaseId> phaseOf(Entity entity, int depth) {
        if (entity == null || depth >= 8) return Optional.empty();
        if (entity instanceof Player player) return Optional.of(playerPhase(player));
        if (entity instanceof Projectile projectile && projectile.getOwner() != null
                && projectile.getOwner() != entity) {
            return phaseOf(projectile.getOwner(), depth + 1);
        }
        if (entity instanceof PartEntity<?> part && part.getParent() != entity) {
            return phaseOf(part.getParent(), depth + 1);
        }
        var mob = entity.getExistingData(ModPhaseAttachments.MOB_PHASE);
        if (mob.isPresent()) return mob.map(state -> state.phaseId());
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            var object = entity.getExistingData(ModPhaseAttachments.PHASE_OBJECT);
            if (object.isPresent()) return object.map(state -> state.phaseId());
        }
        return entity.getExistingData(ModPhaseAttachments.BOSS_ENTITY_LINK)
                .filter(state -> state.valid()).map(state -> state.phaseId());
    }

    private PhaseMembership() {}
}
