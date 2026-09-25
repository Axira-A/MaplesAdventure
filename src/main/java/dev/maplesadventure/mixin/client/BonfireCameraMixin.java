package dev.maplesadventure.mixin.client;

import dev.maplesadventure.client.bonfire.BonfireCameraBridge;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply only the registered visual override AFTER vanilla and Shoulder Surfing finish camera setup. */
@Mixin(Camera.class)
abstract class BonfireCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);

    @Inject(method = "setup", at = @At("RETURN"))
    private void maplesadventure$bonfireCamera(CallbackInfo ci) {
        var pose = BonfireCameraBridge.afterSetup((Camera) (Object) this);
        if (pose == null) return;
        setPosition(pose.position());
        setRotation(pose.yaw(), pose.pitch(), pose.roll());
    }
}
