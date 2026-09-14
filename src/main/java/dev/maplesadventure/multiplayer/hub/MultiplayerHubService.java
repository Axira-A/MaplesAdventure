package dev.maplesadventure.multiplayer.hub;

import dev.maplesadventure.multiplayer.coop.CoopSession;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.coop.SummonSignManager;
import dev.maplesadventure.multiplayer.coop.SummonSignType;
import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import dev.maplesadventure.multiplayer.hub.network.MultiplayerHubPayloads;
import dev.maplesadventure.multiplayer.invasion.InvasionQueueManager;
import dev.maplesadventure.multiplayer.invasion.InvasionSession;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionManager;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Routes menu intents into existing server authorities and returns a fresh projection. */
public final class MultiplayerHubService {
    public static void handle(ServerPlayer player, MultiplayerAction action) {
        switch (action) {
            case PLACE_COOP_SIGN -> SummonSignManager.place(player, SummonSignType.COOP);
            case REMOVE_COOP_SIGN -> SummonSignManager.remove(player, SummonSignType.COOP);
            case PLACE_DUEL_SIGN -> SummonSignManager.place(player, SummonSignType.DUEL);
            case REMOVE_DUEL_SIGN -> SummonSignManager.remove(player, SummonSignType.DUEL);
            case SEEK_INVASION -> InvasionQueueManager.seek(player);
            case CANCEL_INVASION -> InvasionQueueManager.cancel(player);
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new MultiplayerHubPayloads.State(snapshot(player)));
    }

    public static MultiplayerHubState snapshot(ServerPlayer player) {
        PlayerPhaseState phase = PhaseManager.state(player);
        CoopSession coop = CoopSessionManager.session(player.getUUID()).orElse(null);
        InvasionSession hostile = InvasionSessionManager.sessionForPhase(phase.phaseId()).orElse(null);
        boolean formallyInHostile = hostile != null && (hostile.hostUuid().equals(player.getUUID())
                || hostile.invaderUuid().equals(player.getUUID())
                || hostile.hasCooperator() && hostile.cooperatorUuid().equals(player.getUUID()));
        String cooperator = coop != null && coop.hostUuid().equals(player.getUUID())
                ? playerName(player, coop.cooperatorUuid()) : "";
        String invader = formallyInHostile && !hostile.invaderUuid().equals(player.getUUID())
                ? playerName(player, hostile.invaderUuid()) : "";
        return new MultiplayerHubState(
                phase.role(),
                coop != null,
                formallyInHostile,
                InvasionQueueManager.isQueued(player.getUUID()),
                SummonSignManager.findOwner(player.getUUID(), SummonSignType.COOP).isPresent(),
                SummonSignManager.findOwner(player.getUUID(), SummonSignType.DUEL).isPresent(),
                EncounterManager.hasActiveBossAttempt(player.server, phase.phaseId()),
                phase.role() == dev.maplesadventure.multiplayer.phase.PhaseRole.SOLO
                        || phase.role() == dev.maplesadventure.multiplayer.phase.PhaseRole.HOST,
                SummonSignManager.canPlace(player),
                InvasionQueueManager.isQueued(player.getUUID())
                        || dev.maplesadventure.multiplayer.invasion.InvasionEligibilityPolicy.invader(player)
                        == dev.maplesadventure.multiplayer.invasion.InvasionEligibilityPolicy.Failure.NONE,
                cooperator,
                invader
        );
    }

    private static String playerName(ServerPlayer requester, java.util.UUID uuid) {
        ServerPlayer player = requester.server.getPlayerList().getPlayer(uuid);
        return player == null ? "" : player.getGameProfile().getName();
    }

    private MultiplayerHubService() {}
}
