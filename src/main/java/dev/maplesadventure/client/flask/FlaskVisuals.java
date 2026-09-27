package dev.maplesadventure.client.flask;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.maplesadventure.api.flask.FlaskKind;
import dev.maplesadventure.flask.FlaskItems;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Presentation only. Never changes the local attachment, item components or server allocation. */
final class FlaskVisuals {
    private static final ModelResourceLocation[][] MODELS = new ModelResourceLocation[2][7];
    private static final ResourceLocation[][] ICONS = new ResourceLocation[2][7];
    private static final ResourceLocation[] GLOW = new ResourceLocation[8];
    private static final ResourceLocation SILHOUETTE = id("textures/gui/flask/silhouette.png");
    private static ShaderInstance alphaMask;
    private static final int[] HP_WIDTH = {281, 270, 276, 273, 277, 275, 280};
    private static final int[] HP_HEIGHT = {424, 422, 422, 425, 427, 423, 428};
    static {
        for (var kind : FlaskKind.values()) for (int frame = 1; frame <= 7; frame++) {
            String name = kind == FlaskKind.CRIMSON ? "crimson" : "ashen";
            String prefix = kind == FlaskKind.CRIMSON ? "hp" : "mp";
            MODELS[kind.ordinal()][frame - 1] = ModelResourceLocation.standalone(id("item/flask/" + name + "_body_" + frame));
            ICONS[kind.ordinal()][frame - 1] = id("textures/gui/flask/icons/" + prefix + "flask_" + frame + ".png");
        }
        for (int i = 0; i < GLOW.length; i++) GLOW[i] = id("textures/gui/flask/glow_" + (i + 1) + ".png");
    }

    static void registerModels(ModelEvent.RegisterAdditional event) {
        for (var variants : MODELS) for (var model : variants) event.register(model);
    }

    static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("flask_alpha_mask"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), shader -> alphaMask = shader);
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException("Unable to load Flask silhouette shader", error);
        }
    }

    static ResourceLocation iconLocation(FlaskKind kind, int frame) { return ICONS[kind.ordinal()][Math.clamp(frame, 1, 7) - 1]; }
    static float aspect(FlaskKind kind, int frame) {
        int i = Math.clamp(frame, 1, 7) - 1;
        return kind == FlaskKind.CRIMSON ? (float) HP_WIDTH[i] / HP_HEIGHT[i] : 256F / 375F;
    }

    static void model(GuiGraphics g, FlaskKind kind, int frame, int cx, int cy, int size) {
        var mc = Minecraft.getInstance();
        var model = mc.getModelManager().getModel(MODELS[kind.ordinal()][Math.clamp(frame, 1, 7) - 1]);
        g.flush();
        g.pose().pushPose();
        try {
            g.pose().translate(cx, cy, 150);
            g.pose().scale(size, -size, size);
            g.pose().mulPose(Axis.YP.rotationDegrees((float) Math.sin(Util.getMillis() / 2400.0) * 8));
            Lighting.setupForFlatItems();
            mc.getItemRenderer().render(new ItemStack(kind == FlaskKind.CRIMSON ? FlaskItems.CRIMSON.get() : FlaskItems.ASHEN.get()),
                    ItemDisplayContext.GUI, false, g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, model);
            g.flush();
        } finally {
            g.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }

    static void glow(GuiGraphics g, FlaskKind kind, int cx, int cy, int size, boolean lit) {
        int frame = (int) ((Util.getMillis() / 110) % GLOW.length);
        boolean crimson = kind == FlaskKind.CRIMSON;
        drawTexture(g, GLOW[frame], false, true, cx - size / 2F, cy - size / 2F, size, size,
                0, 0, 1, 1, crimson ? 1 : .5F, crimson ? .88F : .82F, crimson ? .5F : 1, lit ? .24F : .04F);
    }

    static void marker(GuiGraphics g, FlaskKind kind, boolean filled, int x, int y, int height) {
        // Sample the actual silhouette bounds. Ignore source RGB (black); retain its original alpha.
        int color = !filled ? 0xFF777D86 : kind == FlaskKind.CRIMSON ? 0xFFFFD270 : 0xFF77CFFF;
        drawTexture(g, SILHOUETTE, true, false, x, y, height * 790F / 1162, height,
                232F / 1254, 46F / 1254, 1022F / 1254, 1208F / 1254,
                (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, filled ? 1 : .55F);
    }

    private static void drawTexture(GuiGraphics g, ResourceLocation texture, boolean mask, boolean additive,
                                    float x, float y, float width, float height, float u0, float v0, float u1, float v1,
                                    float red, float green, float blue, float alpha) {
        if (mask && alphaMask == null) return; // resource reload has not delivered the shader yet
        g.flush();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float[] color = RenderSystem.getShaderColor().clone();
        var shader = RenderSystem.getShader();
        try {
            RenderSystem.enableBlend(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA,
                    additive ? GL11.GL_ZERO : GL11.GL_ONE, additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(mask ? () -> alphaMask : GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture); RenderSystem.setShaderColor(1, 1, 1, 1);
            var matrix = g.pose().last().pose();
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vertices.addVertex(matrix, x, y, 0).setUv(u0, v0).setColor(red, green, blue, alpha);
            vertices.addVertex(matrix, x, y + height, 0).setUv(u0, v1).setColor(red, green, blue, alpha);
            vertices.addVertex(matrix, x + width, y + height, 0).setUv(u1, v1).setColor(red, green, blue, alpha);
            vertices.addVertex(matrix, x + width, y, 0).setUv(u1, v0).setColor(red, green, blue, alpha);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]); RenderSystem.setShader(() -> shader);
            RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthMask);
        }
    }

    static void icon(GuiGraphics g, FlaskKind kind, int frame, int x, int y, int height) {
        int i = Math.clamp(frame, 1, 7) - 1;
        int w = kind == FlaskKind.CRIMSON ? HP_WIDTH[i] : 256;
        int h = kind == FlaskKind.CRIMSON ? HP_HEIGHT[i] : 375;
        int width = Math.round(height * (float) w / h);
        g.blit(ICONS[kind.ordinal()][i], x, y, width, height, 0, 0, w, h, w, h);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("maplesadventure", path); }
    private FlaskVisuals() {}
}
