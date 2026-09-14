package dev.maplesadventure.multiplayer.encounter.fog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.encounter.fog.BossFogGateBlock;
import dev.maplesadventure.multiplayer.encounter.fog.BossFogGateBlockEntity;
import dev.maplesadventure.multiplayer.encounter.fog.PhaseFogGatePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Per-frame alpha only: no block tick, packet, NBT change, or chunk rebuild participates in breathing. */
public final class BossFogGateRenderer implements BlockEntityRenderer<BossFogGateBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "textures/block/fog_gate_block.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final int FULL_BRIGHT = 0x00F000F0;

    public BossFogGateRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public void render(BossFogGateBlockEntity entity, float partialTick, PoseStack poseStack,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (!PhaseFogGatePolicy.shouldRenderClient(entity.getBlockPos()) || entity.getLevel() == null) return;
        float time = entity.getLevel().getGameTime() + partialTick;
        int alpha = Mth.clamp(Math.round((0.75F + 0.15F * Mth.sin(time * Mth.TWO_PI / 60.0F)) * 255.0F), 0, 255);
        Direction facing = entity.getBlockState().getValue(BossFogGateBlock.FACING);
        VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        float inset = 0.501F;
        if (facing.getAxis() == Direction.Axis.Z) {
            quad(consumer, pose, 0, 0, inset, 1, 1, inset, alpha, 0, 0, facing == Direction.NORTH ? -1 : 1);
            quad(consumer, pose, 1, 0, inset, 0, 1, inset, alpha, 0, 0, facing == Direction.NORTH ? 1 : -1);
        } else {
            quad(consumer, pose, inset, 0, 0, inset, 1, 1, alpha, facing == Direction.WEST ? -1 : 1, 0, 0);
            quad(consumer, pose, inset, 0, 1, inset, 1, 0, alpha, facing == Direction.WEST ? 1 : -1, 0, 0);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, float x0, float y0, float z0,
                             float x1, float y1, float z1, int alpha, float nx, float ny, float nz) {
        vertex(out, pose, x0, y0, z0, 0, 1, alpha, nx, ny, nz);
        vertex(out, pose, x1, y0, z1, 1, 1, alpha, nx, ny, nz);
        vertex(out, pose, x1, y1, z1, 1, 0, alpha, nx, ny, nz);
        vertex(out, pose, x0, y1, z0, 0, 0, alpha, nx, ny, nz);
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int alpha, float nx, float ny, float nz) {
        out.addVertex(pose, x, y, z).setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
    }
}
