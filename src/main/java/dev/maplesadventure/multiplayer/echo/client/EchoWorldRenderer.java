package dev.maplesadventure.multiplayer.echo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.maplesadventure.config.EchoClientConfig;
import dev.maplesadventure.multiplayer.echo.EchoFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class EchoWorldRenderer {
    private static EchoPlayerRenderer renderer;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !EchoClientConfig.ENABLED.get()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        if (renderer == null) renderer = new EchoPlayerRenderer();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (EchoPlayback playback : EchoPlaybackManager.active()) {
            EchoFrame frame = playback.frame(partial);
            AABB bounds = new AABB(frame.x() - 0.6D, frame.y(), frame.z() - 0.6D,
                    frame.x() + 0.6D, frame.y() + 2.0D, frame.z() + 0.6D);
            if (!event.getFrustum().isVisible(bounds)) continue;
            float alpha = (float) (EchoClientConfig.ALPHA.get() * playback.alpha(partial));
            poseStack.pushPose();
            poseStack.translate(frame.x() - camera.x, frame.y() - camera.y, frame.z() - camera.z);
            renderer.render(poseStack, buffers, frame, playback.appearance(), alpha);
            poseStack.popPose();
        }
        buffers.endBatch();
    }
    private EchoWorldRenderer() {}
}
