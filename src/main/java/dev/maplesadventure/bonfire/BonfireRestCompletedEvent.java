package dev.maplesadventure.bonfire;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Fired once after an authoritative, successfully committed rest. */
public final class BonfireRestCompletedEvent extends Event {
    private final ServerPlayer player;
    private final BonfireRef bonfire;
    public BonfireRestCompletedEvent(ServerPlayer player, BonfireRef bonfire) {
        this.player = player;
        this.bonfire = bonfire;
    }
    public ServerPlayer player() { return player; }
    public BonfireRef bonfire() { return bonfire; }
}
