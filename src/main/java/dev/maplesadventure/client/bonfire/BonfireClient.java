package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.bonfire.BonfireSessionState;
import dev.maplesadventure.bonfire.network.BonfirePayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Transient view only. Server retains all access decisions and progression data. */
public final class BonfireClient {
    private static BonfirePayloads.View current;
    public static void view(BonfirePayloads.View view) {
        current = view;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BonfireScreen || view.state() == BonfireSessionState.OPEN_STANDING)
            mc.setScreen(new BonfireScreen(view));
        else if (view.state() == BonfireSessionState.RESTING && !(mc.screen instanceof dev.maplesadventure.progression.client.LevelUpScreen))
            mc.setScreen(new BonfireScreen(view));
    }
    public static void closed(BonfirePayloads.Closed closed) {
        if (current == null || !current.nonce().equals(closed.nonce())) return;
        current = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BonfireScreen) mc.setScreen(null);
    }
    public static void activated(BonfirePayloads.Activated packet) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable("message.maplesadventure.bonfire.activated",
                packet.name().isBlank() ? Component.translatable("block.maplesadventure.bonfire") : packet.name()), true);
    }
    public static void request(BonfirePayloads.ActionType action) {
        if (current != null) PacketDistributor.sendToServer(new BonfirePayloads.Action(current.nonce(), action));
    }
    public static boolean resting() { return current != null && current.state() == BonfireSessionState.RESTING; }
    public static boolean resume() {
        if (!resting()) return false;
        Minecraft.getInstance().setScreen(new BonfireScreen(current));
        return true;
    }
    public static void clear() { current = null; }
    private BonfireClient() {}
}
