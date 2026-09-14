package dev.maplesadventure.integration.epicfight.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.client.render.PhantomTint;
import dev.maplesadventure.client.render.PhantomTintingMultiBufferSource;
import dev.maplesadventure.config.EchoClientConfig;
import dev.maplesadventure.multiplayer.echo.EchoAppearanceSnapshot;
import dev.maplesadventure.multiplayer.echo.EchoFrame;
import dev.maplesadventure.multiplayer.echo.EpicFightEchoFrameState;
import dev.maplesadventure.multiplayer.echo.client.EpicFightEchoRenderBridge;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

/** Pure visual Epic Fight armature renderer. It creates no Entity, PlayerPatch or Level registration. */
public final class EpicFightEchoRenderer implements EpicFightEchoRenderBridge.Renderer {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private final HumanoidArmature armature = (HumanoidArmature) Armatures.BIPED.get().deepCopy();
    private final Set<ResourceLocation> warnedUnknown = new HashSet<>();

    @Override
    public boolean render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, EchoFrame frame,
                          EchoAppearanceSnapshot appearance, PlayerSkin skin, float alpha) {
        Pose pose = evaluate(frame.epicFight());
        if (pose == null) return false;
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        if (alphaByte <= 0) return true;

        HumanoidMesh body = (skin.model() == PlayerSkin.Model.SLIM ? Meshes.ALEX : Meshes.BIPED).get();
        body.initialize();
        OpenMatrix4f[] matrices = armature.getPoseAsTransformMatrix(pose, false);
        MultiBufferSource ghostBuffers = new PhantomTintingMultiBufferSource(
                buffers, PhantomTint.RESIDUAL_ECHO, alphaByte);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - frame.bodyYaw()));
        float scale = 0.9375F * EchoClientConfig.MODEL_SCALE.get().floatValue() / 1.2F;
        poseStack.scale(scale, scale, scale);
        body.draw(poseStack, ghostBuffers, RenderType.entityTranslucent(skin.texture()), FULL_BRIGHT,
                1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, matrices);
        renderArmor(poseStack, ghostBuffers, body, appearance, matrices);
        renderHeldItem(poseStack, ghostBuffers, appearance.mainHand(), InteractionHand.MAIN_HAND,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, pose);
        renderHeldItem(poseStack, ghostBuffers, appearance.offHand(), InteractionHand.OFF_HAND,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, pose);
        poseStack.popPose();
        return true;
    }

    private Pose evaluate(EpicFightEchoFrameState state) {
        Pose composed = new Pose();
        boolean any = false;
        for (EpicFightEchoFrameState.LayerState layer : state.layers()) {
            DynamicAnimation animation = resolve(layer.animationId());
            if (animation == null) return null;
            Pose layerPose = animation.getRawPose(layer.elapsedTime());
            if (layer.previousAnimationId() != null && layer.blend() < 1.0F) {
                DynamicAnimation previous = resolve(layer.previousAnimationId());
                if (previous == null) return null;
                layerPose = Pose.interpolatePose(previous.getRawPose(layer.previousElapsedTime()),
                        layerPose, layer.blend());
            }
            composed.load(layerPose, any ? Pose.LoadOperation.OVERWRITE : Pose.LoadOperation.SET);
            any = true;
        }
        return any ? composed : null;
    }

    private DynamicAnimation resolve(ResourceLocation id) {
        try {
            AssetAccessor<? extends DynamicAnimation> accessor = AnimationManager.byKey(id);
            if (accessor == null || accessor.isEmpty()) {
                debugUnknown(id);
                return null;
            }
            return accessor.get();
        } catch (RuntimeException exception) {
            debugUnknown(id);
            return null;
        }
    }

    private void debugUnknown(ResourceLocation id) {
        if (EchoClientConfig.DEBUG.get() && warnedUnknown.add(id)) {
            MaplesAdventure.LOGGER.debug("Unknown Epic Fight echo animation {}; falling back to vanilla", id);
        }
    }

    private void renderArmor(PoseStack poseStack, MultiBufferSource buffers, HumanoidMesh body,
                             EchoAppearanceSnapshot appearance, OpenMatrix4f[] matrices) {
        renderArmorPiece(poseStack, buffers, body, appearance.head(), EquipmentSlot.HEAD, matrices);
        renderArmorPiece(poseStack, buffers, body, appearance.chest(), EquipmentSlot.CHEST, matrices);
        renderArmorPiece(poseStack, buffers, body, appearance.legs(), EquipmentSlot.LEGS, matrices);
        renderArmorPiece(poseStack, buffers, body, appearance.feet(), EquipmentSlot.FEET, matrices);
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffers, HumanoidMesh body,
                                  ItemStack stack, EquipmentSlot slot, OpenMatrix4f[] matrices) {
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) return;
        try {
            SkinnedMesh armorMesh = body.getHumanoidArmorModel(slot).get();
            armorMesh.initialize();
            boolean inner = slot == EquipmentSlot.LEGS;
            for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
                ResourceLocation texture = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(
                        Minecraft.getInstance().player, stack, layer, inner, slot);
                armorMesh.draw(poseStack, buffers, RenderType.entityTranslucent(texture), FULL_BRIGHT,
                        1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, matrices);
            }
        } catch (RuntimeException exception) {
            if (EchoClientConfig.DEBUG.get()) {
                MaplesAdventure.LOGGER.debug("Could not render Epic Fight echo armor {}", stack.getItem(), exception);
            }
        }
    }

    private void renderHeldItem(PoseStack poseStack, MultiBufferSource buffers, ItemStack stack,
                                InteractionHand hand, ItemDisplayContext context, Pose pose) {
        if (stack.isEmpty()) return;
        OpenMatrix4f correction = new OpenMatrix4f().translate(0.0F, 0.0F, -0.13F)
                .rotateDeg(-90.0F, Vec3f.X_AXIS);
        correction.mulFront(armature.getBoundTransformFor(pose,
                hand == InteractionHand.MAIN_HAND ? armature.toolR : armature.toolL));
        poseStack.pushPose();
        MathUtils.mulStack(poseStack, correction);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(stack, context, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, minecraft.level, 0);
        poseStack.popPose();
    }
}
