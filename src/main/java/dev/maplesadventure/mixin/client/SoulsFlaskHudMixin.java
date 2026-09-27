package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.soulscombathud.SoulsFlaskHooks;
import dev.maplesadventure.integration.soulscombathud.SoulsFlaskRendering;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render beside the real consumable frame, inside the same pose stack; never use screen constants. */
@Pseudo
@Mixin(targets = "net.tablesouls.souls_combat_hud.client.render.EquipmentHudOverlay", remap = false)
abstract class SoulsFlaskHudMixin implements SoulsFlaskHooks.Rendering {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void maples$begin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) { SoulsFlaskRendering.reset(); }

    @Inject(method = "renderItemSlot", at = @At("TAIL"), require = 0)
    private void maples$slot(GuiGraphics g, Minecraft mc, int x, int y, ItemStack stack, int u, CallbackInfo ci) {
        SoulsFlaskRendering.slot(g, x, y, stack, u);
    }
    @Inject(method = "renderWeaponSlot", at = @At("HEAD"), require = 0)
    private void maples$weapon(GuiGraphics g, Minecraft mc, int x, int y, ItemStack stack, boolean preview, CallbackInfo ci) {
        SoulsFlaskRendering.obstacle(x, y, 24, 32);
    }
    @Inject(method = "renderIconSlot", at = @At("HEAD"), require = 0)
    private void maples$spell(GuiGraphics g, int x, int y, ResourceLocation icon, float cooldown, CallbackInfo ci) {
        SoulsFlaskRendering.obstacle(x, y, 24, 32);
    }
    @Inject(method = "renderPreviewItemSlot", at = @At("HEAD"), require = 0)
    private void maples$previewItem(GuiGraphics g, Minecraft mc, int x, int y, ItemStack stack, CallbackInfo ci) {
        SoulsFlaskRendering.obstacle(x, y, 14, 14);
    }
    @Inject(method = "renderPreviewIconSlot", at = @At("HEAD"), require = 0)
    private void maples$previewIcon(GuiGraphics g, int x, int y, ResourceLocation icon, float cooldown, CallbackInfo ci) {
        SoulsFlaskRendering.obstacle(x, y, 14, 14);
    }
    @Inject(method = "drawName", at = @At("HEAD"), require = 0)
    private void maples$name(GuiGraphics g, Minecraft mc, int x, String text, int y, boolean right, float alpha, CallbackInfo ci) {
        if (alpha > 0) SoulsFlaskRendering.name(x, y, mc.font.width(text), mc.font.lineHeight, right);
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"), require = 0)
    private void maples$count(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) { SoulsFlaskRendering.draw(g); }
}
