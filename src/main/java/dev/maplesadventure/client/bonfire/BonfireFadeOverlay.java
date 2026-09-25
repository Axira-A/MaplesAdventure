package dev.maplesadventure.client.bonfire;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/** World-to-rest transition. It never authorizes or commits a rest. */
public final class BonfireFadeOverlay {
    public static void render(GuiGraphics graphics, DeltaTracker ignored) {
        float alpha = BonfireClient.fadeAlpha();
        if (alpha <= 0) return;
        int opacity = Math.clamp(Math.round(alpha * 255), 0, 255);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), opacity << 24);
    }

    private BonfireFadeOverlay() {}
}
