package dev.maplesadventure.progression.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Focusable standard widget with MaplesAdventure's understated charcoal/gold treatment. */
final class SoulsButton extends AbstractButton {
    enum Style { ADJUST, PRIMARY, SECONDARY }
    private final Runnable action;
    private final Style style;

    SoulsButton(int x, int y, int width, int height, Component label, Style style, Runnable action) {
        super(x, y, width, height, label);
        this.style = style;
        this.action = action;
    }
    @Override public void onPress() { if (active) action.run(); }

    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && isHoveredOrFocused();
        int bg = !active ? 0xB5161514 : style == Style.PRIMARY
                ? (hot ? 0xE25B3D15 : 0xD63B2A13) : hot ? 0xE0362B1D : 0xD21C1A18;
        int border = !active ? 0xFF49433B : hot ? 0xFFF2B75D
                : style == Style.PRIMARY ? 0xFFB98237 : 0xFF79613E;
        g.fillGradient(getX(), getY(), getRight(), getBottom(), bg, bg & 0xFFEFEFEF);
        g.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
        if (hot && getHeight() > 14) {
            g.fill(getX() + 2, getY() + 2, getRight() - 2, getY() + 3, 0x407ED5FF);
        }
        int color = !active ? 0xFF6E6961 : hot ? 0xFFFFE2AA
                : style == Style.PRIMARY ? 0xFFEBC98D : 0xFFBBB2A5;
        var font = Minecraft.getInstance().font;
        g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2,
                getY() + Math.max(1, (getHeight() - font.lineHeight) / 2), color);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
