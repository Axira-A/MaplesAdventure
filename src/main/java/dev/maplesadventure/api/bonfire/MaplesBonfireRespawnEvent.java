package dev.maplesadventure.api.bonfire;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Fired only after an actual completed respawn at a validated built-in bonfire destination. Not Clone. */
public final class MaplesBonfireRespawnEvent extends Event {
    private final ServerPlayer player; private final MaplesBonfireRef bonfire;
    public MaplesBonfireRespawnEvent(ServerPlayer player, MaplesBonfireRef bonfire) { this.player=player; this.bonfire=bonfire; }
    public ServerPlayer player() { return player; }
    public MaplesBonfireRef bonfire() { return bonfire; }
}
