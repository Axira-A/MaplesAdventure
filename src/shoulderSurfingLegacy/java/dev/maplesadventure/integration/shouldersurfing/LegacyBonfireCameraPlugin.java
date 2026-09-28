package dev.maplesadventure.integration.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.callback.ICameraRotationSetupCallback;
import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingRegistrar;
import com.mojang.logging.LogUtils;
import dev.maplesadventure.client.bonfire.BonfireCameraBridge;
import dev.maplesadventure.client.bonfire.BonfireCameraController;
import dev.maplesadventure.client.bonfire.BonfireCameraController.Rotation;

/** Compiled against 4.x in isolation; only its loader reads the singular entrypoint key. */
public final class LegacyBonfireCameraPlugin implements IShoulderSurfingPlugin {
    @Override public void register(IShoulderSurfingRegistrar registrar) {
        var controller = new BonfireCameraController(
                () -> ShoulderSurfing.getInstance().isShoulderSurfing(),
                () -> {
                    var camera = ShoulderSurfing.getInstance().getCamera();
                    return new Rotation(camera.getXRot(), camera.getYRot());
                });
        registrar.registerCameraRotationSetupCallback(new ICameraRotationSetupCallback() {
            @Override public void post(CameraRotationSetupContext context, CameraRotationSetupResult result) {
                var rotation = controller.rotationOverride();
                if (rotation == null) return;
                result.setXRot(rotation.pitch());
                result.setYRot(rotation.yaw());
            }
        });
        BonfireCameraBridge.register(controller::afterSetup);
        LogUtils.getLogger().info("Bonfire camera adapter enabled (Shoulder Surfing 4.x callbacks)");
    }
}
