package dev.maplesadventure.multiplayer.coop;

import dev.maplesadventure.multiplayer.coop.network.CoopPayloads;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SummonSignSyncService {
    public static final double SYNC_RADIUS = 64.0D;
    private static final Map<UUID, SyncState> STATES = new HashMap<>();

    public static void tick(ServerPlayer player) {
        if ((player.tickCount % 20) != 0) return;
        ChunkPos chunk = player.chunkPosition();
        SyncState previous = STATES.get(player.getUUID());
        long now = player.level().getGameTime();
        if (previous == null || !previous.dimension().equals(player.level().dimension())
                || previous.chunkX() != chunk.x || previous.chunkZ() != chunk.z
                || now - previous.lastSyncTick() >= 100L) syncNow(player);
    }

    public static void syncNow(ServerPlayer player) {
        List<SummonSignSummary> signs = SummonSignManager.nearby(player, SYNC_RADIUS).stream()
                .map(SummonSignRecord::summary).toList();
        PacketDistributor.sendToPlayer(player, new CoopPayloads.SignSnapshot(signs));
        ChunkPos chunk = player.chunkPosition();
        STATES.put(player.getUUID(), new SyncState(player.level().dimension(), chunk.x, chunk.z,
                player.level().getGameTime()));
    }

    public static void broadcastUpsert(ServerLevel level, SummonSignRecord sign) {
        PacketDistributor.sendToPlayersNear(level, null, sign.position().x, sign.position().y, sign.position().z,
                SYNC_RADIUS, new CoopPayloads.SignUpsert(sign.summary()));
    }

    public static void broadcastRemove(ServerLevel level, SummonSignRecord sign) {
        if (level == null) return;
        PacketDistributor.sendToPlayersNear(level, null, sign.position().x, sign.position().y, sign.position().z,
                SYNC_RADIUS, new CoopPayloads.SignRemove(sign.signId()));
    }

    public static void forget(UUID playerUuid) { STATES.remove(playerUuid); }
    public static void clear() { STATES.clear(); }

    private record SyncState(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                             int chunkX, int chunkZ, long lastSyncTick) {}
    private SummonSignSyncService() {}
}
