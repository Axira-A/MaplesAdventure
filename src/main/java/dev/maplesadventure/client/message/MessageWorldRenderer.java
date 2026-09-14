package dev.maplesadventure.client.message;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.message.MessageSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws cached records directly as fullbright ground quads; no Entity or world light is created. */
public final class MessageWorldRenderer {
    public static final ResourceLocation RUNE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "textures/message/message_rune.png"
    );
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(RUNE_TEXTURE);
    private static final float HALF_WIDTH = 0.72F;
    private static final float HALF_DEPTH = 0.24F;
    private static final int FULL_BRIGHT = 0x00F000F0;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !InteractionConfig.MESSAGE_RUNE_ENABLED.get()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Vec3 camera = event.getCamera().getPosition();
        double maxDistanceSqr = Mth.square(InteractionConfig.MESSAGE_RENDER_DISTANCE.get());
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
        PoseStack poseStack = event.getPoseStack();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float pulse = 0.875F + 0.125F * Mth.sin((minecraft.level.getGameTime() + partial) * 0.06F);
        int alpha = Mth.clamp((int) Math.round(255.0D * InteractionConfig.MESSAGE_RUNE_ALPHA.get() * pulse), 0, 255);

        for (MessageSummary message : MessageRenderCache.all()) {
            if (message.position().distanceToSqr(camera) > maxDistanceSqr) continue;
            AABB visibleBounds = new AABB(message.position().subtract(0.8D, 0.05D, 0.8D),
                    message.position().add(0.8D, 0.12D, 0.8D));
            if (!event.getFrustum().isVisible(visibleBounds)) continue;

            poseStack.pushPose();
            poseStack.translate(
                    message.position().x - camera.x,
                    message.position().y - camera.y,
                    message.position().z - camera.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(-message.yaw()));
            PoseStack.Pose pose = poseStack.last();
            vertex(consumer, pose, -HALF_WIDTH, 0.0F, -HALF_DEPTH, 0.0F, 0.0F, alpha);
            vertex(consumer, pose, -HALF_WIDTH, 0.0F, HALF_DEPTH, 0.0F, 1.0F, alpha);
            vertex(consumer, pose, HALF_WIDTH, 0.0F, HALF_DEPTH, 1.0F, 1.0F, alpha);
            vertex(consumer, pose, HALF_WIDTH, 0.0F, -HALF_DEPTH, 1.0F, 0.0F, alpha);
            poseStack.popPose();
        }
        buffers.endBatch(RENDER_TYPE);
    }

    private static void vertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v, int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private MessageWorldRenderer() {
    }
}
