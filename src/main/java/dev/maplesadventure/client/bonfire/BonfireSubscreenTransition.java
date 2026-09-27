package dev.maplesadventure.client.bonfire;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/** Composite a whole child page, including text and item models, once; never captures/blurs the world. */
public final class BonfireSubscreenTransition implements AutoCloseable {
    private final BonfireSubscreenMotion motion = new BonfireSubscreenMotion();
    private TextureTarget target;
    private Runnable afterExit;

    public boolean interactive() { return motion.interactive(); }
    public boolean exiting() { return motion.exiting(); }
    public void exit(Runnable afterExit) {
        if (motion.exiting()) return;
        this.afterExit = afterExit;
        motion.beginExit();
    }
    public void tick() {
        if (motion.claimCompletion() && afterExit != null) {
            Runnable action = afterExit; afterExit = null;
            action.run();
        }
    }

    public void render(GuiGraphics g, Runnable page) {
        float alpha = motion.alpha();
        if (alpha <= 0) return;
        var window = Minecraft.getInstance().getWindow();
        g.flush();
        int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float[] color = RenderSystem.getShaderColor().clone();
        var shader = RenderSystem.getShader();
        try {
            if (target == null) target = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
            else if (target.width != window.getWidth() || target.height != window.getHeight())
                target.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            page.run();
            g.flush();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            // The page is already premultiplied against transparent black. Do not multiply its alpha twice.
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            float x = motion.offset(), w = g.guiWidth(), h = g.guiHeight();
            var matrix = g.pose().last().pose();
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vertices.addVertex(matrix, x, 0, 0).setUv(0, 1).setColor(alpha, alpha, alpha, alpha);
            vertices.addVertex(matrix, x, h, 0).setUv(0, 0).setColor(alpha, alpha, alpha, alpha);
            vertices.addVertex(matrix, x + w, h, 0).setUv(1, 0).setColor(alpha, alpha, alpha, alpha);
            vertices.addVertex(matrix, x + w, 0, 0).setUv(1, 1).setColor(alpha, alpha, alpha, alpha);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.setShader(() -> shader);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthMask);
        }
    }

    @Override public void close() {
        if (target != null) { target.destroyBuffers(); target = null; }
    }
}
