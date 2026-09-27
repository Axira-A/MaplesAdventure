package dev.maplesadventure.integration.soulscombathud;

import dev.maplesadventure.api.flask.FlaskKind;
import dev.maplesadventure.client.flask.FlaskClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Per-render-frame context, populated at actual slot calls inside Souls HUD's pose transform. */
public final class SoulsFlaskRendering {
    private static final List<FlaskHudLayout.Rect> OCCUPIED = new ArrayList<>();
    private static FlaskHudLayout.Rect counter;
    private static String text;
    private static int color;
    public static void reset() { OCCUPIED.clear(); counter = null; text = null; }
    public static void obstacle(int x, int y, int width, int height) {
        OCCUPIED.add(new FlaskHudLayout.Rect(x - width / 2F, y - height / 2F, width, height));
    }
    public static void slot(GuiGraphics g, int x, int y, ItemStack stack, int frameU) {
        obstacle(x, y, 24, 32);
        if (!SoulsFlaskClient.available() || frameU != 48 || !FlaskClient.hasSnapshot()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack selected = SoulsFlaskClient.selected(mc.player);
        FlaskKind kind = SoulsFlaskClient.kind(selected);
        if (kind == null || !ItemStack.isSameItemSameComponents(selected, stack)) return;
        text = FlaskHudPolicy.label(FlaskClient.state(), kind);
        color = kind == FlaskKind.CRIMSON ? 0xFFFFD9A0 : 0xFFAFDFFF;
        counter = FlaskHudLayout.counter(x, y, mc.font.width(text), mc.font.lineHeight);
        if (!fits(g, counter) || OCCUPIED.stream().anyMatch(r -> r.intersects(counter, 1))) counter = null;
    }
    public static void name(int x, int y, int width, int height, boolean right) {
        OCCUPIED.add(new FlaskHudLayout.Rect(right ? x + 12 - width : x - 12, y, width, height));
    }
    public static void draw(GuiGraphics g) {
        if (counter == null || !SoulsFlaskClient.available() || OCCUPIED.stream().anyMatch(r -> r.intersects(counter, 1))) return;
        g.drawString(Minecraft.getInstance().font, text, (int) counter.x(), (int) counter.y(), color, true);
    }
    private static boolean fits(GuiGraphics g, FlaskHudLayout.Rect rect) {
        var matrix = g.pose().last().pose();
        return FlaskHudLayout.fits(rect, matrix.m00(), matrix.m30(), matrix.m31(), g.guiWidth(), g.guiHeight());
    }
    private SoulsFlaskRendering() {}
}
