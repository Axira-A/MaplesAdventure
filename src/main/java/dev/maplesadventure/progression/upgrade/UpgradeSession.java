package dev.maplesadventure.progression.upgrade;

import dev.maplesadventure.progression.PlayerAttributeState;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** In-memory, short-lived capability. Re-login or a new access point always receives a new nonce. */
public final class UpgradeSession {
    final ServerPlayer player;
    final UUID playerUuid;
    final UpgradeAccessContext context;
    long revision;
    boolean editing;
    PlayerAttributeState baseline;
    int baselineXp, baselineCap;
    double baselineMultiplier;

    UpgradeSession(ServerPlayer player, UpgradeAccessContext context) {
        this.player = player;
        this.playerUuid = player.getUUID();
        this.context = context;
    }

    public UUID nonce() { return context.nonce(); }
    public UpgradeAccessContext context() { return context; }
}
