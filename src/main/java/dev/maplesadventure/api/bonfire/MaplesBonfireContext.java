package dev.maplesadventure.api.bonfire;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-thread callback context. Configuration and phase ID are captured before callbacks;
 * the player is a live server entity. No mutable bonfire/session/attachment is exposed.
 */
public record MaplesBonfireContext(ServerPlayer player, MaplesBonfireView bonfire, UUID phaseId) {
    public MaplesBonfireContext {
        Objects.requireNonNull(player);
        Objects.requireNonNull(bonfire);
        Objects.requireNonNull(phaseId);
    }
}
