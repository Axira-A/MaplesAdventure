package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.bonfire.BonfireSessionState;
import dev.maplesadventure.bonfire.BonfireTransitionMath;
import dev.maplesadventure.bonfire.network.BonfirePayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Transient view only. Server retains all access decisions and progression data. */
public final class BonfireClient {
    private static BonfirePayloads.View current;
    /** Immutable server-authored view for optional visual integrations; not an authorization API. */
    public static BonfirePayloads.View currentView() { return current; }
    private static java.util.Set<dev.maplesadventure.bonfire.BonfireRef> activated = java.util.Set.of();
    public static void progress(BonfirePayloads.Progress progress) { activated = java.util.Set.copyOf(progress.activated()); }
    public static boolean isActivated(dev.maplesadventure.bonfire.BonfireRef ref) { return activated.contains(ref); }
    private static long fadeStartedNanos;
    private static long presentationFrameNanos;
    /** Camera and blackout must sample the same frame time, including at low frame rates. */
    public static void beginPresentationFrame() { presentationFrameNanos = System.nanoTime(); }
    public static double transitionElapsedTicks() {
        long now = presentationFrameNanos >= fadeStartedNanos ? presentationFrameNanos : System.nanoTime();
        return (now - fadeStartedNanos) / 50_000_000.0;
    }
    private static BonfirePayloads.View fading;
    public static void view(BonfirePayloads.View view) {
        current = view;
        Minecraft mc = Minecraft.getInstance();
        if (view.state() == BonfireSessionState.SITTING_DOWN) {
            fading = view;
            fadeStartedNanos = System.nanoTime();
        } else if (view.state() == BonfireSessionState.RESTING) {
            fading = null;
            if (!(mc.screen instanceof dev.maplesadventure.progression.client.LevelUpScreen))
                mc.setScreen(new BonfireScreen(view));
        } else if (view.state() == BonfireSessionState.STANDING_UP && mc.screen instanceof BonfireScreen screen) {
            screen.beginLeaving();
        } else if (mc.screen instanceof BonfireScreen) {
            mc.setScreen(null);
        }
    }
    public static void closed(BonfirePayloads.Closed closed) {
        if (current == null || !current.nonce().equals(closed.nonce())) return;
        current = null;
        fading = null;
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
    /** Keep inputs blocked after closing the menu until the server acknowledges the completed stand-up. */
    public static boolean busy() { return current != null && current.state() != BonfireSessionState.CLOSED; }
    public static void onMovement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (!busy()) return;
        var input = event.getInput();
        input.forwardImpulse = 0; input.leftImpulse = 0;
        input.up = false; input.down = false; input.left = false; input.right = false;
        input.jumping = false; input.shiftKeyDown = false;
        dev.maplesadventure.bonfire.BonfirePoseLock.stopMotion(event.getEntity());
    }
    public static void onInteraction(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (busy()) { event.setCanceled(true); event.setSwingHand(false); }
    }
    public static boolean resume() {
        if (!resting()) return false;
        Minecraft.getInstance().setScreen(new BonfireScreen(current));
        return true;
    }
    public static float fadeAlpha() {
        BonfirePayloads.View view = fading;
        if (view == null) return 0;
        double elapsed = transitionElapsedTicks();
        return BonfireTransitionMath.fadeAlpha(elapsed, view.transitionTicks(),
                view.commitTick(), view.fadeInTick());
    }
    public static void clear() { current = null; fading = null; activated = java.util.Set.of(); }
    private BonfireClient() {}
}
