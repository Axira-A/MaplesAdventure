package dev.maplesadventure.progression.client;

import dev.maplesadventure.integration.bonfires.client.BonfiresUpgradeMenuAdapter;
import dev.maplesadventure.progression.BatchUpgradeStatus;
import dev.maplesadventure.progression.network.UpgradePayloads;
import dev.maplesadventure.progression.upgrade.*;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Access-neutral client state. Optional integrations only expose entry widgets. */
public final class UpgradeClient {
    private static UpgradeView offer;
    private static UUID opening;

    public static void register() {
        NeoForge.EVENT_BUS.register(new UpgradeClient());
        NeoForge.EVENT_BUS.register(new BonfiresUpgradeMenuAdapter());
    }
    public static boolean hasOffer(UpgradeAccessType type) {
        return offer != null && offer.access().type() == type;
    }
    public static boolean isOpening() { return opening != null; }
    public static void requestOpen(UpgradeAccessType type) {
        if (!hasOffer(type) || opening != null) return;
        opening = offer.nonce();
        PacketDistributor.sendToServer(new UpgradePayloads.Open(opening));
    }
    @SubscribeEvent public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { clearOffer(false); }

    public static void receive(UpgradePayloads.View payload) {
        var mc = Minecraft.getInstance();
        UpgradeView view = payload.view();
        if (mc.level == null || !mc.level.dimension().location().equals(view.access().sourceDimension())) return;
        if (payload.mode() == UpgradePayloads.Mode.OFFER) {
            offer = view; opening = null; return;
        }
        if (payload.mode() == UpgradePayloads.Mode.OPEN) {
            if (!view.nonce().equals(opening)) {
                PacketDistributor.sendToServer(new UpgradePayloads.Close(view.nonce()));
                return;
            }
        } else if (payload.mode() != UpgradePayloads.Mode.DIRECT_OPEN
                && payload.mode() != UpgradePayloads.Mode.RESULT) return;

        if (payload.mode() == UpgradePayloads.Mode.OPEN || payload.mode() == UpgradePayloads.Mode.DIRECT_OPEN) {
            offer = view; opening = null;
            ClientAttributeState.update(view.attributes());
            mc.setScreen(new LevelUpScreen(view));
            return;
        }
        if (mc.screen instanceof LevelUpScreen screen && screen.nonce().equals(view.nonce())) {
            ClientAttributeState.update(view.attributes());
            offer = view;
            screen.accept(view, payload.status());
        }
    }

    public static void closed(UUID nonce, BatchUpgradeStatus reason) {
        if (offer != null && offer.nonce().equals(nonce)) offer = null;
        if (nonce.equals(opening)) opening = null;
        if (Minecraft.getInstance().screen instanceof LevelUpScreen screen && screen.nonce().equals(nonce))
            screen.invalidate(reason);
    }
    public static void close(UUID nonce) {
        if (Minecraft.getInstance().getConnection() != null)
            PacketDistributor.sendToServer(new UpgradePayloads.Close(nonce));
        closed(nonce, BatchUpgradeStatus.INVALID_SESSION);
    }
    public static void clearOffer(boolean send) {
        if (send && offer != null) close(offer.nonce());
        offer = null; opening = null;
    }
    private UpgradeClient() {}
}
