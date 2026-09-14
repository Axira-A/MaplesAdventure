package dev.maplesadventure.multiplayer.encounter.fog.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Visual/input-side assistance only. The server owns pass, timeout, crossing and activation. */
public final class FogTraversalClientState {
    private static Direction direction;
    private static int remainingTicks;

    public static void start(Direction inside, int ticks) {
        direction = inside;
        remainingTicks = Math.max(0, Math.min(40, ticks));
    }
    public static void stop() { direction = null; remainingTicks = 0; }
    public static void tick() {
        if (direction == null || remainingTicks-- <= 0) { stop(); return; }
        var player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null) { stop(); return; }
        Vec3 move = Vec3.atLowerCornerOf(direction.getNormal()).scale(0.14D);
        player.setDeltaMovement(move.x, player.getDeltaMovement().y, move.z);
    }
    private FogTraversalClientState() {}
}
