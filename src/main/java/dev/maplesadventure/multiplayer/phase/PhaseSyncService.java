package dev.maplesadventure.multiplayer.phase;

import dev.maplesadventure.multiplayer.phase.network.PhasePayloads;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Event-driven synchronization; no per-tick broadcasts or pairwise scans. */
public final class PhaseSyncService {
    public static void login(ServerPlayer player) {
        sendSnapshot(player);
        PacketDistributor.sendToAllPlayers(new PhasePayloads.Update(entry(player)));
        dev.maplesadventure.multiplayer.encounter.fog.FogGateSyncService.syncNow(player);
    }

    public static void changed(ServerPlayer player) {
        PacketDistributor.sendToAllPlayers(new PhasePayloads.Update(entry(player)));
        dev.maplesadventure.multiplayer.encounter.fog.FogGateSyncService.syncNow(player);
    }

    public static void remove(java.util.UUID playerUuid) {
        PacketDistributor.sendToAllPlayers(new PhasePayloads.Remove(playerUuid));
    }

    public static void sendSnapshot(ServerPlayer player) {
        List<PhasePayloads.Entry> entries = PhaseManager.snapshot().entrySet().stream()
                .map(entry -> new PhasePayloads.Entry(entry.getKey(), entry.getValue())).toList();
        PacketDistributor.sendToPlayer(player, new PhasePayloads.Snapshot(entries));
    }

    private static PhasePayloads.Entry entry(ServerPlayer player) {
        return new PhasePayloads.Entry(player.getUUID(), PhaseManager.state(player));
    }

    private PhaseSyncService() {}
}
