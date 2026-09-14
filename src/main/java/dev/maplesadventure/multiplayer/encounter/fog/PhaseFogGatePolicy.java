package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.PhaseEncounterState;
import dev.maplesadventure.multiplayer.encounter.fog.client.FogGateClientCache;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Single source for phase-specific visibility/collision; the real BlockState never changes. */
public final class PhaseFogGatePolicy {
    public static boolean canEntityPass(Entity entity, BlockPos pos) {
        if (!(entity instanceof Player player)) return false;
        if (entity.level().isClientSide()) {
            return FogGateClientCache.at(pos).map(FogGateClientView::passable).orElse(false);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) return false;
        EncounterSavedData data = EncounterSavedData.get(serverPlayer.getServer());
        FogGateDefinition gate = data.fogGate(serverPlayer.level().dimension(), pos).orElse(null);
        if (gate == null) return false;
        if (PhaseManager.state(serverPlayer).role() == PhaseRole.INVADER) return false;
        PhaseEncounterState state = data.state(PhaseManager.state(serverPlayer).phaseId(),
                data.definition(gate.bossEncounterId()).orElseThrow());
        return state.status() == EncounterStatus.DEFEATED
                || FogTraversalManager.hasPass(serverPlayer.getUUID(), gate.gateId());
    }

    public static boolean shouldRenderClient(BlockPos pos) {
        if (!FogGateClientCache.initialized()) return false;
        return FogGateClientCache.at(pos).map(FogGateClientView::render).orElse(true);
    }

    private PhaseFogGatePolicy() {}
}
