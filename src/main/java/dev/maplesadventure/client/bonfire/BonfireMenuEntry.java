package dev.maplesadventure.client.bonfire;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Flat, focusable row rather than a Vanilla stone button. */
final class BonfireMenuEntry extends AbstractButton {
    private final Runnable action;

    BonfireMenuEntry(int x, int y, int width, Component label, Runnable action) {
        super(x, y, width, 22, label);
        this.action = action;
    }

    @Override public void onPress() { if (active) action.run(); }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = isHoveredOrFocused();
        graphics.fill(getX(), getY(), getRight(), getBottom(), tint(selected ? 0xB04B3D2A : 0x7417100C));
        if (selected) graphics.fill(getX(), getY(), getX() + 2, getBottom(), tint(0xFFE1BA76));
        graphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 12,
                getY() + (getHeight() - Minecraft.getInstance().font.lineHeight) / 2,
                tint(selected ? 0xFFFFE4B6 : 0xFFE5D9C4), false);
    }

    private int tint(int color) {
        return dev.maplesadventure.bonfire.BonfireTransitionMath.withOpacity(color, alpha);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
