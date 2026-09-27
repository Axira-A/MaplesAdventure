package dev.maplesadventure.client.flask;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.maplesadventure.flask.FlaskItem;
import dev.maplesadventure.flask.FlaskRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** GUI-only direct texture. Non-power-of-two supplied art must never lower the global atlas mip level. */
final class FlaskIconRenderer extends BlockEntityWithoutLevelRenderer {
    FlaskIconRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof FlaskItem item)) return;
        var kind = item.kind();
        var state = FlaskClient.state();
        int frame = FlaskRules.icon(state.remaining(kind), state.allocated(kind));
        float halfWidth = FlaskVisuals.aspect(kind, frame) / 2;
        var vertices = buffers.getBuffer(RenderType.entityTranslucent(FlaskVisuals.iconLocation(kind, frame)));
        var p = pose.last();
        vertices.addVertex(p, .5F - halfWidth, 0, .5F).setColor(-1).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
        vertices.addVertex(p, .5F + halfWidth, 0, .5F).setColor(-1).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
        vertices.addVertex(p, .5F + halfWidth, 1, .5F).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
        vertices.addVertex(p, .5F - halfWidth, 1, .5F).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
    }
}
