package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.multiplayer.coop.CoopSession;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** FIFO invader queue. Each matchmaking pass materializes the active-host list once. */
public final class InvasionQueueManager {
    private static final Map<UUID, Long> QUEUED_AT = new LinkedHashMap<>();

    public static synchronized void toggle(ServerPlayer player) {
        if (QUEUED_AT.remove(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable("invasion.maplesadventure.queue.cancelled"), true);
            return;
        }
        seek(player);
    }

    public static synchronized boolean seek(ServerPlayer player) {
        if (QUEUED_AT.containsKey(player.getUUID())) return true;
        InvasionEligibilityPolicy.Failure failure = InvasionEligibilityPolicy.invader(player);
        if (failure != InvasionEligibilityPolicy.Failure.NONE) {
            player.displayClientMessage(Component.translatable("invasion.maplesadventure.failure."
                    + failure.name().toLowerCase(java.util.Locale.ROOT)), true);
            return false;
        }
        QUEUED_AT.put(player.getUUID(), System.currentTimeMillis());
        player.displayClientMessage(Component.translatable("invasion.maplesadventure.queue.seeking"), true);
        return true;
    }

    public static synchronized boolean cancel(ServerPlayer player) {
        if (QUEUED_AT.remove(player.getUUID()) == null) return false;
        player.displayClientMessage(Component.translatable("invasion.maplesadventure.queue.cancelled"), true);
        return true;
    }

    public static synchronized void tick(MinecraftServer server) {
        QUEUED_AT.keySet().removeIf(uuid -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            return player == null || InvasionEligibilityPolicy.invader(player) != InvasionEligibilityPolicy.Failure.NONE;
        });
        if (QUEUED_AT.isEmpty()) return;
        List<CoopSession> hosts = new ArrayList<>();
        for (CoopSession session : CoopSessionManager.activeSessions()) {
            if (InvasionEligibilityPolicy.host(server, session) == InvasionEligibilityPolicy.Failure.NONE) hosts.add(session);
        }
        if (hosts.isEmpty()) return;
        java.util.Collections.shuffle(hosts);
        List<Map.Entry<UUID, Long>> queue = QUEUED_AT.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue)).toList();
        int hostIndex = 0;
        for (var entry : queue) {
            if (hostIndex >= hosts.size()) break;
            ServerPlayer invader = server.getPlayerList().getPlayer(entry.getKey());
            if (invader == null) continue;
            InvasionSessionManager.StartResult result = InvasionSessionManager.start(invader, hosts.get(hostIndex));
            if (result == InvasionSessionManager.StartResult.SUCCESS) hostIndex++;
            else if (result != InvasionSessionManager.StartResult.HOST_INELIGIBLE
                    && result != InvasionSessionManager.StartResult.CLAIMED) remove(invader.getUUID());
        }
    }
    public static synchronized boolean isQueued(UUID player) { return QUEUED_AT.containsKey(player); }
    public static synchronized void remove(UUID player) { QUEUED_AT.remove(player); }
    public static synchronized void clear() { QUEUED_AT.clear(); }
    private InvasionQueueManager() {}
}
