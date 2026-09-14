package dev.maplesadventure.client.soul;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.soul.LostSoulEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/** Renders only the owner's head and hat layer with a translucent blue soul tint. */
public final class LostSoulRenderer extends EntityRenderer<LostSoulEntity> {
    private final SkullModel model;

    public LostSoulRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new SkullModel(context.bakeLayer(ModelLayers.PLAYER_HEAD));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            LostSoulEntity soul,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float age = soul.tickCount + partialTick;
        float bob = (float) Math.sin(age * LostSoulVisualStyle.BOB_SPEED) * LostSoulVisualStyle.BOB_AMPLITUDE;
        float rotation = age * LostSoulVisualStyle.ROTATION_DEGREES_PER_TICK;
        float alpha = InteractionConfig.LOST_SOUL_HEAD_ALPHA.get().floatValue();
        int tint = FastColor.ARGB32.colorFromFloat(
                alpha,
                LostSoulVisualStyle.RED,
                LostSoulVisualStyle.GREEN,
                LostSoulVisualStyle.BLUE
        );

        poseStack.pushPose();
        poseStack.translate(0.0D, LostSoulVisualStyle.HEAD_BASE_HEIGHT + bob, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(-LostSoulVisualStyle.HEAD_SCALE, -LostSoulVisualStyle.HEAD_SCALE, LostSoulVisualStyle.HEAD_SCALE);
        model.setupAnim(0.0F, 0.0F, 0.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(soul)));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, tint);
        poseStack.popPose();

        super.render(soul, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected boolean shouldShowName(LostSoulEntity soul) {
        return InteractionConfig.LOST_SOUL_NAMEPLATE.get() && super.shouldShowName(soul);
    }

    @Override
    public ResourceLocation getTextureLocation(LostSoulEntity soul) {
        GameProfile profile = soul.getOwnerProfile();
        return profile == null
                ? DefaultPlayerSkin.get(soul.getUUID()).texture()
                : Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
    }
}
