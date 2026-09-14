package dev.maplesadventure.progression.upgrade;

import dev.maplesadventure.config.ProgressionConfig;
import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionManager;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.network.UpgradePayloads;
import dev.maplesadventure.soul.ExperiencePoints;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sole lifecycle and authorization boundary shared by all level-up access adapters. */
public final class UpgradeAccessService {
    private static final Map<UUID, UpgradeSession> SESSIONS = new HashMap<>();
    private static final long LIFETIME_TICKS = 20L * 180L;
    private static final UUID ADMIN_SOURCE = new UUID(0L, 0L);

    static {
        UpgradeAccessRegistry.register(UpgradeAccessType.ADMIN, (player, context) -> true);
    }

    public static boolean contextAllowed(ServerPlayer player) {
        var phase = PhaseManager.state(player);
        return UpgradeAccessPolicy.allows(phase.role(), player.isAlive() && !player.isRemoved(),
                player.isSpectator(), EncounterManager.hasActiveBossAttempt(player.server, phase.phaseId()),
                InvasionSessionManager.sessionForPhase(phase.phaseId()).isPresent());
    }

    public static void authorize(ServerPlayer player, UpgradeAccessType type, ResourceLocation dimension,
                                 BlockPos position, ResourceLocation sourceKey, UUID sourceId) {
        authorize(player, type, dimension, position, sourceKey, sourceId, false);
    }

    public static void authorizeAndOpen(ServerPlayer player, UpgradeAccessType type,
                                        ResourceLocation dimension, BlockPos position,
                                        ResourceLocation sourceKey, UUID sourceId) {
        authorize(player, type, dimension, position, sourceKey, sourceId, true);
    }

    public static void authorizeAdmin(ServerPlayer player) {
        authorizeAndOpen(player, UpgradeAccessType.ADMIN, player.level().dimension().location(),
                player.blockPosition(), ResourceLocation.fromNamespaceAndPath("maplesadventure", "admin"), ADMIN_SOURCE);
    }

    private static void authorize(ServerPlayer player, UpgradeAccessType type, ResourceLocation dimension,
                                  BlockPos position, ResourceLocation sourceKey, UUID sourceId, boolean direct) {
        close(player);
        var validator = UpgradeAccessRegistry.validator(type);
        if (validator == null || !contextAllowed(player)) return;
        var context = new UpgradeAccessContext(type, dimension, position.immutable(), sourceKey, sourceId,
                UUID.randomUUID(), player.server.getTickCount());
        if (!validator.validate(player, context)) return;
        var session = new UpgradeSession(player, context);
        session.editing = direct;
        SESSIONS.put(player.getUUID(), session);
        send(player, session, direct ? UpgradePayloads.Mode.DIRECT_OPEN : UpgradePayloads.Mode.OFFER,
                BatchUpgradeStatus.SUCCESS);
    }

    public static void open(ServerPlayer player, UUID nonce) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null || !session.nonce().equals(nonce)) { invalid(player, nonce, BatchUpgradeStatus.INVALID_SESSION); return; }
        if (!contextAllowed(player)) { close(player, BatchUpgradeStatus.INVALID_CONTEXT); return; }
        if (!valid(player, session)) { close(player); return; }
        session.editing = true;
        send(player, session, UpgradePayloads.Mode.OPEN, BatchUpgradeStatus.SUCCESS);
    }

    public static void submit(ServerPlayer player, UUID nonce, long revision, Map<Attribute, Integer> deltas) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null || !session.nonce().equals(nonce)) { invalid(player, nonce, BatchUpgradeStatus.INVALID_SESSION); return; }
        BatchUpgradeStatus result = PlayerAttributeService.upgradeBatch(player, deltas, UpgradeContext.ACCESS,
                session, revision);
        if (result == BatchUpgradeStatus.INVALID_SESSION || result == BatchUpgradeStatus.INVALID_CONTEXT) close(player, result);
        else send(player, session, UpgradePayloads.Mode.RESULT, result);
    }

    public static BatchUpgradeStatus validateForUpgrade(ServerPlayer player, UpgradeSession session, long revision) {
        if (!contextAllowed(player)) return BatchUpgradeStatus.INVALID_CONTEXT;
        if (!valid(player, session) || !session.editing) return BatchUpgradeStatus.INVALID_SESSION;
        var state = PlayerAttributeService.state(player);
        if (session.revision != revision || session.baseline == null
                || !session.baseline.values().equals(state.values())
                || session.baselineXp != ExperiencePoints.capture(player)
                || session.baselineCap != ProgressionConfig.ATTRIBUTE_HARD_CAP.get()
                || Double.compare(session.baselineMultiplier, ProgressionConfig.XP_COST_MULTIPLIER.get()) != 0)
            return BatchUpgradeStatus.STALE_STATE;
        return BatchUpgradeStatus.SUCCESS;
    }

    private static boolean valid(ServerPlayer player, UpgradeSession session) {
        if (session == null || SESSIONS.get(player.getUUID()) != session || session.player != player
                || player.server.getTickCount() - session.context.openedAt() >= LIFETIME_TICKS
                || !contextAllowed(player)
                || !player.level().dimension().location().equals(session.context.sourceDimension())) return false;
        var validator = UpgradeAccessRegistry.validator(session.context.type());
        return validator != null && validator.validate(player, session.context);
    }

    private static void send(ServerPlayer player, UpgradeSession session,
                             UpgradePayloads.Mode mode, BatchUpgradeStatus status) {
        var snapshot = PlayerAttributeService.snapshot(player);
        session.baseline = snapshot.state().cleanCopy();
        session.baselineXp = ExperiencePoints.capture(player);
        session.baselineCap = ProgressionConfig.ATTRIBUTE_HARD_CAP.get();
        session.baselineMultiplier = ProgressionConfig.XP_COST_MULTIPLIER.get();
        var view = new UpgradeView(session.context, ++session.revision, snapshot, session.baselineXp,
                session.baselineCap, session.baselineMultiplier);
        PacketDistributor.sendToPlayer(player, new UpgradePayloads.View(mode, status, view));
    }

    private static void invalid(ServerPlayer player, UUID nonce, BatchUpgradeStatus reason) {
        PacketDistributor.sendToPlayer(player, new UpgradePayloads.Closed(nonce, reason));
    }
    public static void close(ServerPlayer player, UUID nonce) {
        var session = SESSIONS.get(player.getUUID());
        if (session != null && session.nonce().equals(nonce)) close(player);
    }
    public static void close(ServerPlayer player) { close(player, BatchUpgradeStatus.INVALID_SESSION); }
    private static void close(ServerPlayer player, BatchUpgradeStatus reason) {
        var session = SESSIONS.remove(player.getUUID());
        if (session != null && !player.hasDisconnected()) invalid(player, session.nonce(), reason);
    }
    public static void tick() {
        for (var session : List.copyOf(SESSIONS.values())) {
            if (!contextAllowed(session.player)) close(session.player, BatchUpgradeStatus.INVALID_CONTEXT);
            else if (!valid(session.player, session)) close(session.player);
        }
    }
    public static void clear() { SESSIONS.clear(); }
    public static void refreshPolicyView(ServerPlayer player) {
        var session = SESSIONS.get(player.getUUID());
        if (session != null && session.editing) send(player, session, UpgradePayloads.Mode.RESULT, BatchUpgradeStatus.STALE_STATE);
    }
    private UpgradeAccessService() {}
}
