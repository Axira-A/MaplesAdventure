package dev.maplesadventure.message;

import dev.maplesadventure.config.MessageConfig;
import dev.maplesadventure.network.MessagePayloads;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends bounded nearby snapshots on chunk changes and sparse periodic refreshes. */
public final class MessageSyncService {
    private static final Map<UUID, SyncState> STATES = new HashMap<>();

    public static void tick(ServerPlayer player) {
        if ((player.tickCount % 20) != 0) return;
        ChunkPos chunk = player.chunkPosition();
        SyncState previous = STATES.get(player.getUUID());
        long gameTime = player.level().getGameTime();
        boolean moved = previous == null || previous.chunkX != chunk.x || previous.chunkZ != chunk.z
                || !previous.dimension.equals(player.level().dimension());
        if (moved || gameTime - (previous == null ? Long.MIN_VALUE : previous.lastSyncTick) >= 100L) {
            syncNow(player);
        }
    }

    public static void syncNow(ServerPlayer player) {
        if (!MessageConfig.ENABLED.get()) {
            PacketDistributor.sendToPlayer(player, new MessagePayloads.NearbySnapshot(List.of()));
            return;
        }
        MessageSavedData data = MessageSavedData.get(player.serverLevel().getServer());
        List<AdventureMessage> nearby = data.nearby(
                player.level().dimension(), player.position(), MessageConfig.SYNC_RADIUS.get(),
                MessagePayloads.MAX_SYNC_MESSAGES
        );
        java.util.ArrayList<MessageSummary> summaries = new java.util.ArrayList<>(nearby.size());
        for (AdventureMessage message : nearby) {
            if (player.serverLevel().isLoaded(message.supportPos())
                    && !MessageValidator.supportIsValid(player.serverLevel(), message)) {
                MessageManager.deleteAdministrative(player.serverLevel().getServer(), message.messageId());
            } else {
                summaries.add(message.summary());
            }
        }
        PacketDistributor.sendToPlayer(player, new MessagePayloads.NearbySnapshot(summaries));
        ChunkPos chunk = player.chunkPosition();
        STATES.put(player.getUUID(), new SyncState(
                player.level().dimension(), chunk.x, chunk.z, player.level().getGameTime()
        ));
    }

    public static void broadcastUpsert(ServerLevel level, AdventureMessage message) {
        PacketDistributor.sendToPlayersNear(
                level, null, message.position().x, message.position().y, message.position().z,
                MessageConfig.SYNC_RADIUS.get(), new MessagePayloads.Upsert(message.summary())
        );
    }

    public static void broadcastRemove(ServerLevel level, AdventureMessage message) {
        PacketDistributor.sendToPlayersNear(
                level, null, message.position().x, message.position().y, message.position().z,
                MessageConfig.SYNC_RADIUS.get(), new MessagePayloads.Remove(message.messageId())
        );
    }

    public static void forget(UUID playerUuid) {
        STATES.remove(playerUuid);
    }

    public static void clear() {
        STATES.clear();
    }

    private record SyncState(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                             int chunkX, int chunkZ, long lastSyncTick) {
    }

    private MessageSyncService() {
    }
}
