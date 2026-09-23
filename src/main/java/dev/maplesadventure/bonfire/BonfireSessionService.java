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
    private static final int SIT_TICKS = 36;
    private static final int STAND_TICKS = 30;
    private static boolean upgradeRegistered;

    public static synchronized void registerUpgradeSource() {
        if (upgradeRegistered) return;
        BonfireUpgradeSourceRegistry.register(UPGRADE_SOURCE, BonfireSessionService::validateUpgrade);
        upgradeRegistered = true;
    }

    public static void interact(ServerPlayer player, BlockPos pos) {
        if (!BonfireAccessPolicy.allows(player)) {
            player.displayClientMessage(Component.translatable("message.maplesadventure.bonfire.denied"), true);
            return;
        }
        BonfireBlockEntity bonfire = BonfireStateService.resolve(player.serverLevel(), pos);
        if (bonfire == null || !BonfireStateService.closeEnough(player, pos)) return;
        BonfireRef ref = bonfire.ref();
        PlayerBonfireState state = BonfireStateService.state(player);
        if (!state.isActivated(ref)) {
            if (!state.activate(ref)) return;
            player.setData(dev.maplesadventure.progression.ProgressionAttachments.PLAYER_BONFIRES, state);
            PacketDistributor.sendToPlayer(player, new BonfirePayloads.Activated(bonfire.displayName()));
            return;
        }
        close(player);
        Session session = new Session(player, ref, UUID.randomUUID(), player.server.getTickCount());
        SESSIONS.put(player.getUUID(), session);
        view(session);
    }

    public static void action(ServerPlayer player, UUID nonce, BonfirePayloads.ActionType action) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.nonce.equals(nonce) || !valid(session)) {
            if (session != null) close(player);
            return;
        }
        switch (action) {
            case REST -> {
                if (session.state != BonfireSessionState.OPEN_STANDING) return;
                BonfireBlockEntity bonfire = BonfireStateService.resolve(player.serverLevel(), session.ref.pos());
                if (bonfire == null || !BonfireRestService.rest(player, bonfire)) { close(player); return; }
                session.state = BonfireSessionState.SITTING_DOWN;
                session.stateSince = player.server.getTickCount();
                BonfireAnimationIntegration.play(player, session.state);
                view(session);
            }
            case LEVEL_UP -> {
                if (session.state != BonfireSessionState.RESTING) return;
                BonfireBlockEntity bonfire = BonfireStateService.resolve(player.serverLevel(), session.ref.pos());
                if (bonfire == null || !bonfire.hasFeature(BonfireFeature.LEVEL_UP)) return;
                UpgradeAccessService.authorizeAndOpen(player, UpgradeAccessType.BONFIRE,
                        session.ref.dimension(), session.ref.pos(), UPGRADE_SOURCE, session.ref.generation());
            }
            case LEAVE -> {
                if (session.state == BonfireSessionState.OPEN_STANDING) close(player);
                else if (session.state == BonfireSessionState.RESTING) {
                    session.state = BonfireSessionState.STANDING_UP;
                    session.stateSince = player.server.getTickCount();
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

    private static boolean valid(Session session) {
        ServerPlayer player = session.player;
        return player.connection != null && !player.hasDisconnected()
                && player.server.getTickCount() - session.openedAt <= 20L * 180L
                && BonfireAccessPolicy.allows(player)
                && BonfireStateService.closeEnough(player, session.ref.pos())
                && BonfireStateService.matches(player.serverLevel(), session.ref);
    }

    public static void tick(MinecraftServer server) {
        for (Session session : List.copyOf(SESSIONS.values())) {
            if (!valid(session)) { close(session.player); continue; }
            long elapsed = server.getTickCount() - session.stateSince;
            if (session.state == BonfireSessionState.SITTING_DOWN && elapsed >= SIT_TICKS) {
                session.state = BonfireSessionState.RESTING;
                session.stateSince = server.getTickCount();
                BonfireAnimationIntegration.play(session.player, session.state);
                view(session);
            } else if (session.state == BonfireSessionState.STANDING_UP && elapsed >= STAND_TICKS) close(session.player);
        }
    }

    public static void close(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
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
                bonfire.displayName(), session.state == BonfireSessionState.RESTING && bonfire.hasFeature(BonfireFeature.LEVEL_UP)));
    }
    private static final class Session {
        final ServerPlayer player;
        final BonfireRef ref;
        final UUID nonce;
        final long openedAt;
        BonfireSessionState state = BonfireSessionState.OPEN_STANDING;
        long stateSince;
        Session(ServerPlayer player, BonfireRef ref, UUID nonce, long openedAt) {
            this.player = player; this.ref = ref; this.nonce = nonce; this.openedAt = openedAt;
            this.stateSince = openedAt;
        }
    }
    private BonfireSessionService() {}
}
