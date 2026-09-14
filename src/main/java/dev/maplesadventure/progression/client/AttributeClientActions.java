package dev.maplesadventure.progression.client;

import dev.maplesadventure.progression.network.AttributePayloads;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AttributeClientActions {
    public static void requestSnapshot() { PacketDistributor.sendToServer(new AttributePayloads.RequestSnapshot()); }
    private AttributeClientActions() {}
}
