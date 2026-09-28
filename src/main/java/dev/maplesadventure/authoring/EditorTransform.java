package dev.maplesadventure.authoring;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/** Absolute world transform, with no scale or inherited group transform. Angles are degrees. */
public record EditorTransform(Vec3 position, float yaw, float pitch) {
    public EditorTransform {
        Objects.requireNonNull(position);
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || Math.abs(position.x) > 30_000_000 || Math.abs(position.z) > 30_000_000 || Math.abs(position.y) > 30_000_000
                || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(yaw) > 360 || Math.abs(pitch) > 90)
            throw new IllegalArgumentException("editor.maplesadventure.invalid_transform");
    }
    public static EditorTransform origin() { return new EditorTransform(Vec3.ZERO, 0, 0); }
}
