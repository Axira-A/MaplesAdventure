package dev.maplesadventure.bonfire;

import dev.maplesadventure.bonfire.network.BonfirePayloads;
import dev.maplesadventure.progression.upgrade.BonfireUpgradeSourceRegistry;
import dev.maplesadventure.progression.upgrade.UpgradeAccessContext;
import dev.maplesadventure.progression.upgrade.UpgradeAccessService;
import dev.maplesadventure.progression.upgrade.UpgradeAccessType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Ephemeral, server-authoritative rest/menu state. Progress lives in PlayerBonfireState instead. */
public final class BonfireSessionService {
    public static final ResourceLocation UPGRADE_SOURCE = ResourceLocation.fromNamespaceAndPath("maplesadventure", "bonfire");
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    // Include Epic Fight's 0.12 s blend-in; do not cut off the end of an authored clip.
    private static final int ANIMATED_ACTIVATE_TICKS = 39;
    private static final int FALLBACK_ACTIVATE_TICKS = 10;
    private static final int ANIMATED_SIT_TICKS = 43;
    private static final int FALLBACK_SIT_TICKS = 24;
    private static final int ANIMATED_STAND_TICKS = 33;
    private static final int FALLBACK_STAND_TICKS = 8;
    private static boolean upgradeRegistered;

    public static synchronized void registerUpgradeSource() {
        if (upgradeRegistered) return;
        BonfireUpgradeSourceRegistry.register(UPGRADE_SOURCE, BonfireSessionService::validateUpgrade);
        upgradeRegistered = true;
    }

    public static void interact(ServerPlayer player, BlockPos pos) {
        if (!BonfireAccessPolicy.allows(player) || !player.onGround() || player.isPassenger()) {
            player.displayClientMessage(Component.translatable("message.maplesadventure.bonfire.denied"), true);
            return;
        }
        BonfireBlockEntity bonfire = BonfireStateService.resolve(player.serverLevel(), pos);
        if (bonfire == null || !BonfireStateService.closeEnough(player, pos)) return;
        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null) {
            if (valid(existing)) return;
            close(player);
        }
        BonfireRef ref = bonfire.ref();
        PlayerBonfireState state = BonfireStateService.state(player);
        if (!state.isActivated(ref)) {
            if (!state.activate(ref)) return;
            player.setData(dev.maplesadventure.progression.ProgressionAttachments.PLAYER_BONFIRES, state);
            BonfireStateService.sync(player);
            Session session = new Session(player, ref, BonfireSessionState.ACTIVATING,
                    BonfireAnimationIntegration.available() ? ANIMATED_ACTIVATE_TICKS : FALLBACK_ACTIVATE_TICKS,
                    0, 0);
            SESSIONS.put(player.getUUID(), session);
            BonfirePoseLock.stopMotion(player);
            player.stopUsingItem();
            BonfireAnimationIntegration.play(player, session.state);
            PacketDistributor.sendToPlayer(player, new BonfirePayloads.Activated(bonfire.displayName()));
            view(session);
            return;
        }
        boolean animated = BonfireAnimationIntegration.available();
        Session session = new Session(player, ref, BonfireSessionState.SITTING_DOWN,
                animated ? ANIMATED_SIT_TICKS : FALLBACK_SIT_TICKS,
                animated ? 14 : 10, animated ? 20 : 16);
        SESSIONS.put(player.getUUID(), session);
        BonfirePoseLock.stopMotion(player);
        player.stopUsingItem();
        BonfireAnimationIntegration.play(player, session.state);
        view(session);
    }

    public static void action(ServerPlayer player, UUID nonce, BonfirePayloads.ActionType action, ResourceLocation featureId) {
        BonfireApiBridge.checkThread(player.server);
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.player != player || !session.nonce.equals(nonce) || !valid(session)) {
            if (session != null) close(player);
            return;
        }
        switch (action) {
            case SELECT_FEATURE -> {
                if (session.state != BonfireSessionState.RESTING) return;
                BonfireBlockEntity bonfire = BonfireStateService.resolve(player.serverLevel(), session.ref.pos());
                if (bonfire == null || featureId == null) return;
                var context = BonfireApiBridge.context(player, bonfire);
                if (!BonfireApiBridge.REGISTRY.execute(featureId, bonfire.features(), context))
                    player.displayClientMessage(Component.translatable("message.maplesadventure.bonfire.denied"), true);
            }
            case LEAVE -> {
                if (session.state == BonfireSessionState.RESTING) {
                    session.state = BonfireSessionState.STANDING_UP;
                    session.stateSince = player.server.getTickCount();
                    session.transitionTicks = BonfireAnimationIntegration.available()
                            ? ANIMATED_STAND_TICKS : FALLBACK_STAND_TICKS;
                    BonfireAnimationIntegration.play(player, session.state);
                    view(session);
                }
            }
        }
    }

    private static boolean validateUpgrade(ServerPlayer player, UpgradeAccessContext context) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.state == BonfireSessionState.RESTING && valid(session)
                && context.sourceKey().equals(UPGRADE_SOURCE)
                && context.sourceDimension().equals(session.ref.dimension())
                && context.sourcePosition().equals(session.ref.pos())
                && context.sourceId().equals(session.ref.generation())
                && BonfireStateService.resolve(player.serverLevel(), session.ref.pos()).hasFeature(BonfireFeature.LEVEL_UP);
    }

    /** Only the transition owner may commit rest; consumes the permit before addon callbacks run. */
    static boolean claimRest(ServerPlayer player, BonfireRef ref) {
        BonfireApiBridge.checkThread(player.server);
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.player != player || !session.ref.equals(ref)
                || session.state != BonfireSessionState.SITTING_DOWN || !session.restCommitted
                || session.restExecuted || !valid(session)) return false;
        session.restExecuted = true;
        return true;
    }

    static java.util.Optional<dev.maplesadventure.api.bonfire.MaplesBonfireContext> restingContext(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.player != player || session.state != BonfireSessionState.RESTING
                || !valid(session)) return java.util.Optional.empty();
        return java.util.Optional.of(BonfireApiBridge.context(player,
                BonfireStateService.resolve(player.serverLevel(), session.ref.pos())));
    }

    private static boolean valid(Session session) {
        ServerPlayer player = session.player;
        return player.connection != null && !player.hasDisconnected()
                && player.server.getTickCount() - session.openedAt <= 20L * 180L
                && BonfireAccessPolicy.allows(player)
                && !player.isPassenger()
                // Explicit external teleport ends pose ownership instead of dragging the player back.
                && player.position().distanceToSqr(session.anchor) < 0.0025
                && BonfireStateService.closeEnough(player, session.ref.pos())
                && BonfireStateService.matches(player.serverLevel(), session.ref);
    }

    public static void tick(MinecraftServer server) {
        for (Session session : List.copyOf(SESSIONS.values())) {
            if (!valid(session)) { close(session.player); continue; }
            long elapsed = server.getTickCount() - session.stateSince;
            if (session.state == BonfireSessionState.ACTIVATING && elapsed >= session.transitionTicks) {
                close(session.player);
            } else if (session.state == BonfireSessionState.SITTING_DOWN) {
                if (BonfireTransitionMath.shouldCommit(session.state, elapsed,
                        session.commitTick, session.restCommitted)) {
                    // Claim the one-shot transition before invoking hooks that can re-enter this service.
                    session.restCommitted = true;
                    BonfireBlockEntity bonfire = BonfireStateService.resolve(session.player.serverLevel(), session.ref.pos());
                    if (bonfire == null || !BonfireRestService.rest(session.player, bonfire)) {
                        close(session.player);
                        continue;
                    }
                    // A participant may safely close the session or teleport the player; never resurrect its UI.
                    if (SESSIONS.get(session.player.getUUID()) != session || !valid(session)) {
                        if (SESSIONS.get(session.player.getUUID()) == session) close(session.player);
                        continue;
                    }
                }
                if (elapsed >= session.transitionTicks) {
                    session.state = BonfireSessionState.RESTING;
                    session.stateSince = server.getTickCount();
                    BonfireAnimationIntegration.play(session.player, session.state);
                    view(session);
                }
            } else if (session.state == BonfireSessionState.STANDING_UP && elapsed >= session.transitionTicks) {
                close(session.player);
            }
        }
    }

    /** Server-side combat/interaction lock while the bonfire owns the player's pose. */
    public static boolean isBusy(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void close(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) BonfirePoseLock.stopMotion(player);
        if (session != null && !player.hasDisconnected()) BonfireAnimationIntegration.stop(player);
        if (session != null && !player.hasDisconnected()) {
            UpgradeAccessService.closeForSource(player, UPGRADE_SOURCE, session.ref.generation());
            PacketDistributor.sendToPlayer(player, new BonfirePayloads.Closed(session.nonce));
        }
    }
    public static void clear() { SESSIONS.clear(); }

    private static void view(Session session) {
        BonfireBlockEntity bonfire = BonfireStateService.resolve(session.player.serverLevel(), session.ref.pos());
        if (bonfire == null) { close(session.player); return; }
        PacketDistributor.sendToPlayer(session.player, new BonfirePayloads.View(session.nonce, session.state,
                bonfire.displayName(), session.state == BonfireSessionState.RESTING
                        ? BonfireApiBridge.REGISTRY.available(bonfire.features(), BonfireApiBridge.context(session.player, bonfire))
                            .stream().map(f -> new BonfirePayloads.MenuEntry(f.id(), f.translationKey(), f.order())).toList()
                        : List.of(),
                session.transitionTicks, session.commitTick, session.fadeInTick, session.ref.pos()));
    }
    private static final class Session {
        final ServerPlayer player;
        final BonfireRef ref;
        final UUID nonce = UUID.randomUUID();
        final long openedAt;
        final net.minecraft.world.phys.Vec3 anchor;
        BonfireSessionState state;
        long stateSince;
        int transitionTicks;
        final int commitTick;
        final int fadeInTick;
        boolean restCommitted;
        boolean restExecuted;
        Session(ServerPlayer player, BonfireRef ref, BonfireSessionState state,
                int transitionTicks, int commitTick, int fadeInTick) {
            this.player = player; this.ref = ref; this.state = state;
            this.anchor = player.position();
            this.openedAt = player.server.getTickCount();
            this.stateSince = openedAt;
            this.transitionTicks = transitionTicks;
            this.commitTick = commitTick;
            this.fadeInTick = fadeInTick;
        }
    }
    private BonfireSessionService() {}
}
