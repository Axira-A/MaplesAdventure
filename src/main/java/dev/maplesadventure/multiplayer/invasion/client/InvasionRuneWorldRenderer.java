package dev.maplesadventure.multiplayer.invasion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.maplesadventure.MaplesAdventure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Short-lived fullbright arrival rune; no world Entity or picking surface is created. */
public final class InvasionRuneWorldRenderer {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "textures/invasion/rune_invader.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        PoseStack stack = event.getPoseStack();
        for (var visual : InvasionMaterializationCache.all()) {
            if (visual.position().distanceToSqr(camera) > 48.0D * 48.0D
                    || !event.getFrustum().isVisible(new AABB(visual.position().subtract(1, .05, .5), visual.position().add(1, .1, .5)))) continue;
            int alpha = Mth.clamp(Math.round(245.0F * visual.alpha()), 0, 255);
            stack.pushPose();
            stack.translate(visual.position().x - camera.x, visual.position().y + 0.012D - camera.y,
                    visual.position().z - camera.z);
            stack.mulPose(Axis.YP.rotationDegrees(-visual.yaw()));
            PoseStack.Pose pose = stack.last();
            vertex(consumer, pose, -0.95F, -0.31F, 0, 0, alpha);
            vertex(consumer, pose, -0.95F, 0.31F, 0, 1, alpha);
            vertex(consumer, pose, 0.95F, 0.31F, 1, 1, alpha);
            vertex(consumer, pose, 0.95F, -0.31F, 1, 0, alpha);
            stack.popPose();
        }
        buffers.endBatch(TYPE);
    }
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v, int alpha) {
        consumer.addVertex(pose, x, 0, z).setColor(255, 42, 35, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 1, 0);
    }
    private InvasionRuneWorldRenderer() {}
}
