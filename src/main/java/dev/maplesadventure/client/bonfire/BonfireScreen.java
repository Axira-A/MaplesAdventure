package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.bonfire.BonfireSessionState;
import dev.maplesadventure.bonfire.network.BonfirePayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Left-anchored rest menu; the world remains visible and no Screen blur pass is invoked. */
public final class BonfireScreen extends Screen {
    private final BonfirePayloads.View view;
    private final List<BonfireMenuEntry> entries = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private boolean leaving;
    private long leavingNanos;
    private float leavingOpacity;
    private final long openedNanos = System.nanoTime();

    public BonfireScreen(BonfirePayloads.View view) {
        super(Component.translatable("screen.maplesadventure.bonfire"));
        this.view = view;
    }

    @Override protected void init() {
        panelWidth = Math.min(width - 24, Math.clamp(Math.round(width * 0.23F), 180, 300));
        panelX = Math.max(12, Math.round(width * 0.05F));
        panelY = Math.max(24, Math.round(height * 0.16F));
        entries.clear();
        if (view.state() != BonfireSessionState.RESTING) return;
        int rowY = panelY + 34;
        if (view.canLevelUp()) {
            addEntry(rowY, Component.translatable("screen.maplesadventure.bonfire.level_up"),
                    () -> BonfireClient.request(BonfirePayloads.ActionType.LEVEL_UP));
            rowY += 26;
        }
        addEntry(rowY, Component.translatable("screen.maplesadventure.bonfire.leave"), this::onClose);
        if (!entries.isEmpty()) setFocused(entries.getFirst());
    }

    private void addEntry(int y, Component text, Runnable action) {
        BonfireMenuEntry entry = new BonfireMenuEntry(panelX, y, panelWidth, text, action);
        entries.add(entry);
        addRenderableWidget(entry);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float opacity = opacity();
        // Font treats near-zero alpha as opaque: omit those first frames entirely.
        if (opacity < 0.02F) return;
        int veilWidth = Math.min(width, panelX + panelWidth + 48);
        for (int x = 0; x < veilWidth; x++) {
            float falloff = dev.maplesadventure.bonfire.BonfireTransitionMath.veilOpacity((double) x / (veilWidth - 1));
            graphics.fill(x, 0, x + 1, height, tint(0xB8100E0B, opacity * falloff));
        }
        graphics.fill(panelX, panelY + 27, panelX + panelWidth, panelY + 28, tint(0xAFB99B68, opacity));
        Component name = view.name().isBlank() ? Component.translatable("block.maplesadventure.bonfire")
                : Component.literal(view.name());
        String title = font.plainSubstrByWidth(name.getString(), panelWidth - 4);
        graphics.drawString(font, title, panelX, panelY + 9, tint(0xFFE5D0A5, opacity), false);
        for (BonfireMenuEntry entry : entries) {
            entry.setAlpha(opacity);
            entry.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static int tint(int color, float alpha) {
        return dev.maplesadventure.bonfire.BonfireTransitionMath.withOpacity(color, alpha);
    }

    /** Screen.render calls this implicitly in 1.21.1; external callers must not reintroduce blur either. */
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W
                || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            if (entries.isEmpty()) return true;
            int index = entries.indexOf(getFocused());
            int direction = keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W ? -1 : 1;
            setFocused(entries.get(Math.floorMod(index + direction, entries.size())));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void onClose() {
        if (!leaving) {
            beginLeaving();
            BonfireClient.request(BonfirePayloads.ActionType.LEAVE);
        }
    }

    /** Keep the visual menu alive during fade-out; inputs remain disabled until server stand-up completes. */
    public void beginLeaving() {
        if (leaving) return;
        leavingOpacity = opacity();
        leavingNanos = System.nanoTime();
        leaving = true;
        for (BonfireMenuEntry entry : entries) entry.active = false;
    }

    private float opacity() {
        if (leaving) return leavingOpacity * (1 - dev.maplesadventure.bonfire.BonfireTransitionMath.menuAlpha(
                (System.nanoTime() - leavingNanos) / 1_000_000.0));
        return dev.maplesadventure.bonfire.BonfireTransitionMath.menuAlpha(
                (System.nanoTime() - openedNanos) / 1_000_000.0);
    }

    @Override public void tick() {
        if (leaving && System.nanoTime() - leavingNanos >= 320_000_000L && minecraft.screen == this)
            minecraft.setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }
}
