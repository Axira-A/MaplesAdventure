package dev.maplesadventure.bonfire;

import dev.maplesadventure.multiplayer.encounter.EncounterResetReason;
import dev.maplesadventure.multiplayer.encounter.EncounterResetService;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** Only successful REST goes through this path. Opening a menu or upgrading never resets encounters. */
public final class BonfireRestService {
    public static boolean rest(ServerPlayer player, BonfireBlockEntity bonfire) {
        if (!BonfireAccessPolicy.allows(player) || !BonfireStateService.closeEnough(player, bonfire.getBlockPos())) return false;
        BonfireRef ref = bonfire.ref();
        PlayerBonfireState state = BonfireStateService.state(player);
        if (!state.isActivated(ref) || !state.rest(ref, player.getYRot())) return false;
        player.setData(ProgressionAttachments.PLAYER_BONFIRES, state);
        DerivedStatRuntimeService.restoreToMaximum(player);
        EncounterResetService.resetForPlayer(player, EncounterResetReason.BONFIRE);
        NeoForge.EVENT_BUS.post(new BonfireRestCompletedEvent(player, ref));
        return true;
    }
    private BonfireRestService() {}
}
