package dev.maplesadventure.client.editor;

import net.minecraft.world.phys.Vec3;

/** Stateless authoring displacement, in blocks per tick. No momentum or gameplay attribute changes. */
public final class EditorNavigation {
    public static final double SPEED = 0.35;

    public static Vec3 motion(double yaw, int left, int forward, int vertical) {
        if (!Double.isFinite(yaw)) return Vec3.ZERO;
        var local = new Vec3(Math.clamp(left, -1, 1), Math.clamp(vertical, -1, 1), Math.clamp(forward, -1, 1));
        if (local.lengthSqr() == 0) return Vec3.ZERO;
        local = local.normalize().scale(SPEED);
        double angle = Math.toRadians(yaw), sin = Math.sin(angle), cos = Math.cos(angle);
        return new Vec3(local.x * cos - local.z * sin, local.y, local.z * cos + local.x * sin);
    }

    public static boolean acceptsMovement(boolean active, boolean focused, boolean textInput, boolean modal, boolean manipulating) {
        return active && focused && !textInput && !modal && !manipulating;
    }
    private EditorNavigation() {}
}
