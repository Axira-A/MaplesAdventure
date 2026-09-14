package dev.maplesadventure.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Wraps every vanilla/mod PlayerRenderer layer, including armor, equipment, cape and held items. */
public final class PhantomTintingMultiBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final PhantomTint tint;
    private final int dynamicAlpha;

    public PhantomTintingMultiBufferSource(MultiBufferSource delegate, PhantomTint tint) {
        this(delegate, tint, 255);
    }

    public PhantomTintingMultiBufferSource(MultiBufferSource delegate, PhantomTint tint, int dynamicAlpha) {
        this.delegate = delegate;
        this.tint = tint;
        this.dynamicAlpha = Math.max(0, Math.min(255, dynamicAlpha));
    }

    @Override public VertexConsumer getBuffer(RenderType renderType) {
        return new PhantomTintingVertexConsumer(delegate.getBuffer(renderType), tint, dynamicAlpha);
    }
}
