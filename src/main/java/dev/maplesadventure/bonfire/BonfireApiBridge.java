package dev.maplesadventure.bonfire;

import dev.maplesadventure.api.bonfire.*;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.progression.upgrade.UpgradeAccessService;
import dev.maplesadventure.progression.upgrade.UpgradeAccessType;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Internal bridge. Public signatures remain detached and optional-mod-free. */
public final class BonfireApiBridge {
    static final BonfireExtensionRegistry REGISTRY = new BonfireExtensionRegistry();
    static {
        REGISTRY.register(new MaplesBonfireFeatureHandler() {
            public ResourceLocation id() { return MaplesBonfireFeatures.LEVEL_UP; }
            public String translationKey() { return "screen.maplesadventure.bonfire.level_up"; }
            public int order() { return 100; }
            public boolean isAvailable(MaplesBonfireContext context) { return UpgradeAccessService.contextAllowed(context.player()); }
            public void execute(MaplesBonfireContext context) {
                var ref = context.bonfire().ref();
                UpgradeAccessService.authorizeAndOpen(context.player(), UpgradeAccessType.BONFIRE,
                        ref.dimension(), ref.position(), BonfireSessionService.UPGRADE_SOURCE, ref.generation());
            }
        });
        REGISTRY.register(BonfirePhaseResetService.encounters());
    }
    public static void checkThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Bonfire API requires logical server thread");
    }
    public static MaplesBonfireRef detached(BonfireRef ref) {
        return new MaplesBonfireRef(ref.dimension(), ref.pos(), ref.generation());
    }
    static BonfireRef internal(MaplesBonfireRef ref) {
        return new BonfireRef(ref.dimension(), ref.position(), ref.generation());
    }
    public static MaplesBonfireView detached(BonfireBlockEntity bonfire) {
        return new MaplesBonfireView(detached(bonfire.ref()), bonfire.displayName(), bonfire.features());
    }
    static MaplesBonfireContext context(ServerPlayer player, BonfireBlockEntity bonfire) {
        return new MaplesBonfireContext(player, detached(bonfire), PhaseManager.state(player).phaseId().value());
    }
    public static Optional<MaplesBonfireView> query(ServerLevel level, BlockPos pos) {
        checkThread(level.getServer());
        return Optional.ofNullable(BonfireStateService.resolve(level, pos)).map(BonfireApiBridge::detached);
    }
    public static boolean isActivated(ServerPlayer player, MaplesBonfireRef ref) {
        checkThread(player.server);
        return BonfireStateService.state(player).isActivated(internal(ref));
    }
    public static Optional<MaplesBonfireRef> lastRested(ServerPlayer player) {
        checkThread(player.server);
        return BonfireStateService.state(player).lastRested().map(p -> detached(p.ref()));
    }
    public static boolean isResting(ServerPlayer player) {
        checkThread(player.server);
        return BonfireSessionService.restingContext(player).isPresent();
    }
    public static List<ResourceLocation> available(ServerPlayer player) {
        checkThread(player.server);
        return BonfireSessionService.restingContext(player)
                .map(c -> REGISTRY.available(c.bonfire().configuredFeatures(), c).stream().map(BonfireExtensionRegistry.Feature::id).toList())
                .orElse(List.of());
    }
    public static void registerFeature(MaplesBonfireFeatureHandler handler) { REGISTRY.register(handler); }
    public static void registerReset(MaplesBonfireRestResetParticipant participant) {
        if (participant.priority() == Integer.MIN_VALUE)
            throw new IllegalArgumentException("Minimum priority is reserved for the core encounter reset");
        REGISTRY.register(participant);
    }
    public static boolean registered(ResourceLocation id) { return REGISTRY.registered(id); }
    public static void freeze() { REGISTRY.freeze(); }
    private BonfireApiBridge() {}
}
