package dev.maplesadventure.multiplayer.echo;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Compact visual-only sample. Equipment and profile data intentionally live outside frames. */
public record EchoFrame(
        int serverTick, double x, double y, double z,
        float yaw, float pitch, float bodyYaw, float headYaw,
        float walkPosition, float walkSpeed, float swingProgress,
        byte pose, boolean onGround, EpicFightEchoFrameState epicFight
) {
    public EchoFrame {
        if (epicFight == null) epicFight = EpicFightEchoFrameState.INACTIVE;
    }
    public static EchoFrame interpolate(EchoFrame from, EchoFrame to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        return new EchoFrame(
                Mth.floor(Mth.lerp(t, from.serverTick, to.serverTick)),
                Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.y, to.y), Mth.lerp(t, from.z, to.z),
                Mth.rotLerp(t, from.yaw, to.yaw), Mth.lerp(t, from.pitch, to.pitch),
                Mth.rotLerp(t, from.bodyYaw, to.bodyYaw), Mth.rotLerp(t, from.headYaw, to.headYaw),
                Mth.lerp(t, from.walkPosition, to.walkPosition), Mth.lerp(t, from.walkSpeed, to.walkSpeed),
                Mth.lerp(t, from.swingProgress, to.swingProgress), t < 0.5F ? from.pose : to.pose,
                t < 0.5F ? from.onGround : to.onGround,
                EpicFightEchoFrameState.interpolate(from.epicFight, to.epicFight, t)
        );
    }

    public Vec3 position() { return new Vec3(x, y, z); }
    public Pose resolvedPose() {
        Pose[] poses = Pose.values();
        return pose >= 0 && pose < poses.length ? poses[pose] : Pose.STANDING;
    }
}
