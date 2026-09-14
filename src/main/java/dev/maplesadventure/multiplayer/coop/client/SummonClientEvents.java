package dev.maplesadventure.multiplayer.coop.client;

import dev.maplesadventure.client.MaplesAdventureClient;
import dev.maplesadventure.interaction.InteractionCandidate;
import dev.maplesadventure.interaction.InteractionTargetManager;
import dev.maplesadventure.interaction.SummonSignInteractionTarget;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class SummonClientEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new SummonClientEvents()); }

    @SubscribeEvent
    public void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        InteractionCandidate candidate = InteractionTargetManager.getInstance().getCurrentCandidate();
        if (candidate == null || !(candidate.target() instanceof SummonSignInteractionTarget)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() == InteractionHand.MAIN_HAND) MaplesAdventureClient.interactCurrentTarget();
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SummonSignRenderCache.clear();
    }

    private SummonClientEvents() {}
}
