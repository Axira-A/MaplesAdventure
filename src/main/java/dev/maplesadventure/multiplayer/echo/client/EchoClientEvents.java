package dev.maplesadventure.multiplayer.echo.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class EchoClientEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new EchoClientEvents()); }
    @SubscribeEvent public void onTick(ClientTickEvent.Post event) {
        EchoPlaybackManager.tick();
        EpicFightEchoClientBridge.sampleAndSend();
    }
    @SubscribeEvent public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { EchoPlaybackManager.clear(); }
    private EchoClientEvents() {}
}
