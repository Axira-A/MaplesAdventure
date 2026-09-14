package dev.maplesadventure.multiplayer.echo.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.EchoClientConfig;
import dev.maplesadventure.client.render.PhantomTint;
import dev.maplesadventure.client.render.PhantomTintingMultiBufferSource;
import dev.maplesadventure.client.render.PhantomTintingVertexConsumer;
import dev.maplesadventure.multiplayer.echo.EchoAppearanceSnapshot;
import dev.maplesadventure.multiplayer.echo.EchoFrame;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Dedicated visual renderer. It never constructs or registers a Minecraft Entity. */
public final class EchoPlayerRenderer {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private final PlayerModel<LivingEntity> wide;
    private final PlayerModel<LivingEntity> slim;
    private final HumanoidArmorModel<LivingEntity> innerArmor;
    private final HumanoidArmorModel<LivingEntity> outerArmor;
    private final Map<java.util.UUID, java.util.function.Supplier<PlayerSkin>> skins =
            new java.util.LinkedHashMap<>(32, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<java.util.UUID,
                        java.util.function.Supplier<PlayerSkin>> eldest) { return size() > 128; }
            };
    private final java.util.Set<String> warnedArmor = new java.util.HashSet<>();

    public EchoPlayerRenderer() {
        Minecraft minecraft = Minecraft.getInstance();
        wide = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        innerArmor = new HumanoidArmorModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outerArmor = new HumanoidArmorModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        // EntityModel defaults to young=true. Without an actual entity/setupAnim call it never flips back,
        // causing AgeableListModel to render a baby-zombie-sized body.
        wide.young = false;
        slim.young = false;
        innerArmor.young = false;
        outerArmor.young = false;
    }

    public void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, EchoFrame frame,
                       EchoAppearanceSnapshot appearance, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerSkin skin = resolveSkin(minecraft, appearance);
        PlayerModel<LivingEntity> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        if (alphaByte <= 0) return;
        int ghostColor = FastColor.ARGB32.color(alphaByte, 205, 222, 238);

        if (EpicFightEchoRenderBridge.render(poseStack, buffers, frame, appearance, skin, alpha)) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - frame.bodyYaw()));
        // Preserve the already-generated 1.2 config value as the full-size baseline after fixing young=false.
        float scale = 0.9375F * EchoClientConfig.MODEL_SCALE.get().floatValue() / 1.2F;
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        pose(model, frame);

        VertexConsumer skinConsumer = buffers.getBuffer(RenderType.entityTranslucent(skin.texture()));
        model.renderToBuffer(poseStack, skinConsumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ghostColor);
        renderArmor(poseStack, buffers, model, appearance, ghostColor);
        renderHeldItem(poseStack, buffers, model, appearance.mainHand(), HumanoidArm.RIGHT,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, alphaByte);
        renderHeldItem(poseStack, buffers, model, appearance.offHand(), HumanoidArm.LEFT,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, alphaByte);
        renderCape(poseStack, buffers, model, skin, appearance, ghostColor);
        poseStack.popPose();
    }

    private PlayerSkin resolveSkin(Minecraft minecraft, EchoAppearanceSnapshot appearance) {
        if (minecraft.getConnection() != null) {
            var info = minecraft.getConnection().getPlayerInfo(appearance.playerId());
            if (info != null) return info.getSkin();
        }
        return skins.computeIfAbsent(appearance.playerId(), id -> minecraft.getSkinManager()
                .lookupInsecure(new GameProfile(id, appearance.profileName()))).get();
    }

    private static void pose(PlayerModel<LivingEntity> model, EchoFrame frame) {
        model.setAllVisible(true);
        model.head.resetPose(); model.hat.resetPose(); model.body.resetPose();
        model.rightArm.resetPose(); model.leftArm.resetPose(); model.rightLeg.resetPose(); model.leftLeg.resetPose();
        model.head.yRot = (frame.headYaw() - frame.bodyYaw()) * Mth.DEG_TO_RAD;
        model.head.xRot = Mth.clamp(frame.pitch(), -75.0F, 75.0F) * Mth.DEG_TO_RAD;
        model.hat.copyFrom(model.head);
        float swing = frame.walkPosition();
        float speed = Mth.clamp(frame.walkSpeed(), 0.0F, 1.0F);
        model.rightArm.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 1.1F * speed;
        model.leftArm.xRot = Mth.cos(swing * 0.6662F) * 1.1F * speed;
        model.rightLeg.xRot = Mth.cos(swing * 0.6662F) * 1.4F * speed;
        model.leftLeg.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 1.4F * speed;
        if (!frame.onGround()) {
            model.rightLeg.xRot = 0.3F;
            model.leftLeg.xRot = -0.3F;
        }
        if (frame.swingProgress() > 0.0F) {
            model.rightArm.xRot -= Mth.sin(frame.swingProgress() * Mth.PI) * 1.2F;
        }
        if (frame.resolvedPose() == Pose.CROUCHING) {
            model.body.xRot = 0.5F;
            model.rightArm.xRot += 0.4F; model.leftArm.xRot += 0.4F;
            model.rightLeg.z = 4.0F; model.leftLeg.z = 4.0F;
            model.rightLeg.y = 12.2F; model.leftLeg.y = 12.2F;
            model.head.y = 4.2F; model.body.y = 3.2F;
        }
        model.leftSleeve.copyFrom(model.leftArm); model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg); model.rightPants.copyFrom(model.rightLeg);
        model.jacket.copyFrom(model.body);
    }

    private void renderArmor(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                             PlayerModel<LivingEntity> base, EchoAppearanceSnapshot appearance, int color) {
        renderArmorPiece(poseStack, buffers, base, appearance.head(), EquipmentSlot.HEAD, color);
        renderArmorPiece(poseStack, buffers, base, appearance.chest(), EquipmentSlot.CHEST, color);
        renderArmorPiece(poseStack, buffers, base, appearance.legs(), EquipmentSlot.LEGS, color);
        renderArmorPiece(poseStack, buffers, base, appearance.feet(), EquipmentSlot.FEET, color);
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                  PlayerModel<LivingEntity> base, ItemStack stack, EquipmentSlot slot, int color) {
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) return;
            HumanoidArmorModel<LivingEntity> model = slot == EquipmentSlot.LEGS ? innerArmor : outerArmor;
            base.copyPropertiesTo(model);
            setArmorVisibility(model, slot);
            try {
                boolean inner = slot == EquipmentSlot.LEGS;
                for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
                    var texture = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(
                            Minecraft.getInstance().player, stack, layer, inner, slot);
                    VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
                    model.renderToBuffer(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
                }
            } catch (RuntimeException exception) {
                String key = stack.getItem().toString();
                if (warnedArmor.add(key)) MaplesAdventure.LOGGER.warn("Could not render residual-echo armor {}", key, exception);
            }
    }

    private static void setArmorVisibility(HumanoidModel<LivingEntity> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> { model.head.visible = true; model.hat.visible = true; }
            case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
            case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
            case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
            default -> { }
        }
    }

    private static void renderHeldItem(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                       PlayerModel<LivingEntity> model, ItemStack item, HumanoidArm arm,
                                       ItemDisplayContext context, int alpha) {
        if (item.isEmpty()) return;
        poseStack.pushPose();
        model.translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((arm == HumanoidArm.LEFT ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        MultiBufferSource ghostBuffers = new PhantomTintingMultiBufferSource(
                buffers, PhantomTint.RESIDUAL_ECHO, alpha);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(item, context, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, ghostBuffers, minecraft.level, 0);
        poseStack.popPose();
    }

    private static void renderCape(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                   PlayerModel<LivingEntity> model, PlayerSkin skin,
                                   EchoAppearanceSnapshot appearance, int color) {
        if (skin.capeTexture() == null || appearance.chest().is(net.minecraft.world.item.Items.ELYTRA)) return;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(12.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        int alpha = FastColor.ARGB32.alpha(color);
        model.renderCloak(poseStack, new PhantomTintingVertexConsumer(
                        buffers.getBuffer(RenderType.entityTranslucent(skin.capeTexture())),
                        PhantomTint.RESIDUAL_ECHO, alpha),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
