import dev.maplesadventure.api.bonfire.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** Call register during your mod's initialization/common setup, before any server starts. */
public final class BonfireIntegrationExample {
    public static void register() {
        MaplesBonfireApi.registerFeature(new MaplesBonfireFeatureHandler() {
            public ResourceLocation id() { return ResourceLocation.parse("examplemod:craft"); }
            public String translationKey() { return "feature.examplemod.craft"; }
            public int order() { return 1000; }
            public boolean isAvailable(MaplesBonfireContext context) { return true; }
            public void execute(MaplesBonfireContext context) {
                context.player().displayClientMessage(Component.literal("Example feature selected"), true);
            }
        });
        MaplesBonfireApi.registerRestResetParticipant(new MaplesBonfireRestResetParticipant() {
            public ResourceLocation id() { return ResourceLocation.parse("examplemod:owned_enemies"); }
            public int priority() { return 100; }
            public void reset(MaplesBonfireContext context) {
                // Reset ONLY your registered state belonging to context.phaseId(); never scan nearby shared mobs.
            }
        });
        NeoForge.EVENT_BUS.addListener(BonfireIntegrationExample::completed);
    }
    public static void query(ServerLevel level, BlockPos position, ServerPlayer player) {
        MaplesBonfireApi.query(level, position).ifPresent(view -> {
            boolean activated = MaplesBonfireApi.isActivated(player, view.ref());
            player.displayClientMessage(Component.literal("Activated: " + activated), true);
        });
        MaplesBonfireApi.lastRested(player).ifPresent(ref ->
                player.displayClientMessage(Component.literal("Last rest: " + ref.position()), true));
    }
    private static void completed(MaplesBonfireRestCompletedEvent event) {
        // Read-only notification; no direct rest re-entry API exists.
        var ref = event.context().bonfire().ref();
    }
}
