package dev.maplesadventure.client.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Compact, focusable Souls-style slot/chip. */
final class MessageSlotButton extends AbstractButton {
    private final Runnable action;

    MessageSlotButton(int x, int y, int width, Component label, Runnable action) {
        super(x, y, width, 18, label);
        this.action = action;
    }

    @Override public void onPress() { action.run(); }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = !active ? 0x80302A22 : isHoveredOrFocused() ? 0xE0524027 : 0xC0211B15;
        int border = isHoveredOrFocused() ? 0xFFFFB95B : 0xFF8E6A36;
        graphics.fill(getX(), getY(), getRight(), getBottom(), background);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
        int color = active ? 0xFFFFE1A6 : 0xFF777064;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + 5, color);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
