package dev.maplesadventure.integration.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.event.handler.SetupCameraRotationEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.math.Vec2f;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.mojang.logging.LogUtils;
import dev.maplesadventure.client.bonfire.BonfireCameraBridge;
import dev.maplesadventure.client.bonfire.BonfireCameraController;
import dev.maplesadventure.client.bonfire.BonfireCameraController.Rotation;

/** Selected only by the 5.x plugin loader's entrypoints key. */
public final class BonfireCameraPlugin implements IShoulderSurfingPlugin {
    @Override public void register(IEventBus bus) {
        var controller = new BonfireCameraController(
                () -> IShoulderSurfing.getInstance().isShoulderSurfing(),
                () -> {
                    var camera = IShoulderSurfing.getInstance().getCamera();
                    return new Rotation(camera.getXRot(), camera.getYRot());
                });
        bus.register((SetupCameraRotationEventHandler) event -> {
            var rotation = controller.rotationOverride();
            if (rotation == null) return;
            event.setResult(new Vec2f(rotation.pitch(), rotation.yaw()));
            event.cancel();
        });
        BonfireCameraBridge.register(controller::afterSetup);
        LogUtils.getLogger().info("Bonfire camera adapter enabled (Shoulder Surfing 5.x events)");
    }
}
