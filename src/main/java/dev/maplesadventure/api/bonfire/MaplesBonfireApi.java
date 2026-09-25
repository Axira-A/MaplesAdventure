package dev.maplesadventure.api.bonfire;

import dev.maplesadventure.bonfire.BonfireApiBridge;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Built-in MaplesAdventure bonfires only, not arbitrary third-party bonfires.
 * Queries require the logical server thread and never load chunks.
 * Register extensions during initialization/common setup; duplicate IDs and late registration are rejected.
 * There is intentionally no public rest or mutable session API.
 */
public final class MaplesBonfireApi {
    public static Optional<MaplesBonfireView> query(ServerLevel level, BlockPos position) {
        return BonfireApiBridge.query(level, position);
    }
    public static boolean isActivated(ServerPlayer player, MaplesBonfireRef ref) {
        return BonfireApiBridge.isActivated(player, ref);
    }
    public static Optional<MaplesBonfireRef> lastRested(ServerPlayer player) {
        return BonfireApiBridge.lastRested(player);
    }
    public static boolean isResting(ServerPlayer player) { return BonfireApiBridge.isResting(player); }
    public static void registerFeature(MaplesBonfireFeatureHandler handler) { BonfireApiBridge.registerFeature(handler); }
    public static void registerRestResetParticipant(MaplesBonfireRestResetParticipant participant) {
        BonfireApiBridge.registerReset(participant);
    }
    /** Registration is separate from per-bonfire configuration and per-player availability. */
    public static boolean isFeatureRegistered(ResourceLocation id) { return BonfireApiBridge.registered(id); }
    /** Only actions authorized for this player's current RESTING session, in deterministic menu order. */
    public static List<ResourceLocation> availableFeatures(ServerPlayer player) { return BonfireApiBridge.available(player); }
    private MaplesBonfireApi() {}
}
