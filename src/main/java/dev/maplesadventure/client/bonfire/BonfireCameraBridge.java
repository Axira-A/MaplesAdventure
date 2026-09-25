package dev.maplesadventure.client.bonfire;

import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/** No optional-mod types: without the Shoulder Surfing plugin the camera hook is a no-op. */
public final class BonfireCameraBridge {
    public record Pose(Vec3 position, float yaw, float pitch, float roll) {}
    private static Function<Camera, Pose> provider = camera -> null;
    public static void register(Function<Camera, Pose> value) { provider = value; }
    public static Pose afterSetup(Camera camera) {
        BonfireClient.beginPresentationFrame();
        return provider.apply(camera);
    }
    private BonfireCameraBridge() {}
}
