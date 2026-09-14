package dev.maplesadventure.multiplayer.coop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.coop.SummonSignSummary;
import dev.maplesadventure.multiplayer.coop.SummonSignType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Fullbright transient world quad; no Entity, ticking object, collision or picking surface exists. */
public final class SummonSignWorldRenderer {
    public static final ResourceLocation COOP_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "textures/summon/rune_helper.png"
    );
    public static final ResourceLocation DUEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "textures/invasion/rune_invader.png"
    );
    private static final RenderType COOP_RENDER_TYPE = RenderType.entityTranslucentEmissive(COOP_TEXTURE);
    private static final RenderType DUEL_RENDER_TYPE = RenderType.entityTranslucentEmissive(DUEL_TEXTURE);
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float HALF_WIDTH = 0.86F;
    private static final float HALF_DEPTH = 0.29F;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float pulse = 0.90F + 0.10F * Mth.sin((minecraft.level.getGameTime() + partial) * 0.08F);
        int alpha = Mth.clamp(Math.round(245.0F * pulse), 0, 255);

        for (SummonSignSummary sign : SummonSignRenderCache.all()) {
            if (sign.position().distanceToSqr(camera) > 48.0D * 48.0D) continue;
            AABB bounds = new AABB(sign.position().subtract(1.0D, 0.04D, 0.5D),
                    sign.position().add(1.0D, 0.12D, 0.5D));
            if (!event.getFrustum().isVisible(bounds)) continue;
            poseStack.pushPose();
            poseStack.translate(sign.position().x - camera.x, sign.position().y - camera.y,
                    sign.position().z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-sign.yaw()));
            PoseStack.Pose pose = poseStack.last();
            VertexConsumer consumer = buffers.getBuffer(sign.type() == SummonSignType.DUEL
                    ? DUEL_RENDER_TYPE : COOP_RENDER_TYPE);
            int red = sign.type() == SummonSignType.DUEL ? 255 : 255;
            int green = sign.type() == SummonSignType.DUEL ? 68 : 238;
            int blue = sign.type() == SummonSignType.DUEL ? 52 : 116;
            vertex(consumer, pose, -HALF_WIDTH, -HALF_DEPTH, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, pose, -HALF_WIDTH, HALF_DEPTH, 0.0F, 1.0F, red, green, blue, alpha);
            vertex(consumer, pose, HALF_WIDTH, HALF_DEPTH, 1.0F, 1.0F, red, green, blue, alpha);
            vertex(consumer, pose, HALF_WIDTH, -HALF_DEPTH, 1.0F, 0.0F, red, green, blue, alpha);
            poseStack.popPose();
        }
        buffers.endBatch(COOP_RENDER_TYPE);
        buffers.endBatch(DUEL_RENDER_TYPE);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float z, float u, float v, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, 0.0F, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private SummonSignWorldRenderer() {}
}
