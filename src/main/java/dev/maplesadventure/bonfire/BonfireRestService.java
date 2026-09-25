package dev.maplesadventure.bonfire;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.bonfire.MaplesBonfireRestCompletedEvent;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** Only successful REST goes through this path. Opening a menu or upgrading never resets encounters. */
public final class BonfireRestService {
    public static boolean rest(ServerPlayer player, BonfireBlockEntity bonfire) {
        if (!BonfireAccessPolicy.allows(player) || !BonfireStateService.closeEnough(player, bonfire.getBlockPos())) return false;
        BonfireRef ref = bonfire.ref();
        if (!BonfireStateService.matches(player.serverLevel(), ref) || !BonfireSessionService.claimRest(player, ref)) return false;
        PlayerBonfireState state = BonfireStateService.state(player);
        if (!state.isActivated(ref) || !state.rest(ref, player.getYRot())) return false;
        player.setData(ProgressionAttachments.PLAYER_BONFIRES, state);
        var context = BonfireApiBridge.context(player, bonfire);
        DerivedStatRuntimeService.restoreToMaximum(player);
        if (!BonfirePhaseResetService.reset(context)) return false;
        // Notification failures cannot retry a committed transaction or strand the sitting session.
        try { NeoForge.EVENT_BUS.post(new BonfireRestCompletedEvent(player, ref)); }
        catch (RuntimeException | LinkageError failure) { MaplesAdventure.LOGGER.warn("Internal bonfire rest notification failed", failure); }
        try { NeoForge.EVENT_BUS.post(new MaplesBonfireRestCompletedEvent(context)); }
        catch (RuntimeException | LinkageError failure) { MaplesAdventure.LOGGER.warn("Public bonfire rest notification failed", failure); }
        BonfireStateService.sync(player);
        return true;
    }
    private BonfireRestService() {}
}
