package dev.maplesadventure.multiplayer.invasion.client;

import dev.maplesadventure.multiplayer.invasion.network.InvasionPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InvasionClientActions {
    public static void toggleSeek() { PacketDistributor.sendToServer(new InvasionPayloads.ToggleSeek()); }
    private InvasionClientActions() {}
}
