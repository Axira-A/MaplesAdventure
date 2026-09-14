package dev.maplesadventure.progression;

import dev.maplesadventure.progression.network.AttributePayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Local-player-only, change-driven synchronization. */
public final class AttributeSyncService {
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new AttributePayloads.Snapshot(PlayerAttributeService.snapshot(player)));
    }
    private AttributeSyncService() {}
}
