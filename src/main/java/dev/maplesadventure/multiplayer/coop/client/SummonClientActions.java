package dev.maplesadventure.multiplayer.coop.client;

import dev.maplesadventure.multiplayer.coop.network.CoopPayloads;
import java.util.UUID;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SummonClientActions {
    public static void toggleSign() { PacketDistributor.sendToServer(new CoopPayloads.ToggleSign()); }
    public static void requestSummon(UUID signId) { PacketDistributor.sendToServer(new CoopPayloads.RequestSummon(signId)); }
    public static void leave() { PacketDistributor.sendToServer(new CoopPayloads.Leave()); }
    public static void dismiss() { PacketDistributor.sendToServer(new CoopPayloads.Dismiss()); }
    private SummonClientActions() {}
}
