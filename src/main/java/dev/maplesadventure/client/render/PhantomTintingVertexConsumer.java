package dev.maplesadventure.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Shared per-vertex tint used by residual echoes and real cooperator render layers. */
public final class PhantomTintingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final PhantomTint tint;
    private final int dynamicAlpha;

    public PhantomTintingVertexConsumer(VertexConsumer delegate, PhantomTint tint) {
        this(delegate, tint, 255);
    }

    public PhantomTintingVertexConsumer(VertexConsumer delegate, PhantomTint tint, int dynamicAlpha) {
        this.delegate = delegate;
        this.tint = tint;
        this.dynamicAlpha = Math.max(0, Math.min(255, dynamicAlpha));
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red * tint.red() / 255, green * tint.green() / 255, blue * tint.blue() / 255,
                alpha * tint.alpha() / 255 * dynamicAlpha / 255);
        return this;
    }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) {
        if (tint.fullBright()) delegate.setUv2(240, 240); else delegate.setUv2(u, v);
        return this;
    }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
}
