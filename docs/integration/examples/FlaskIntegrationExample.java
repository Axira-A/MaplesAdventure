package example;

import dev.maplesadventure.api.flask.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** A combat addon owns stagger semantics; it need not import Flask internals or either optional combat mod. */
public final class FlaskIntegrationExample {
    public static void onStagger(ServerPlayer player) { FlaskApi.cancelUse(player,FlaskCancelReason.STAGGER); }
    public static int charges(ServerPlayer player) { return FlaskApi.getCrimsonRemaining(player); }
    public static void register() {
        NeoForge.EVENT_BUS.addListener((FlaskEvents.Effect event)-> {
            if(event.player().getTags().contains("example:flask_bonus")) event.setAmount(event.amount()*1.1);
        });
    }
    private FlaskIntegrationExample() {}
}
