package dev.maplesadventure.client.flask;

import dev.maplesadventure.flask.*;
import dev.maplesadventure.api.flask.FlaskKind;
import dev.maplesadventure.client.bonfire.BonfireClient;
import dev.maplesadventure.client.bonfire.BonfireScreen;
import dev.maplesadventure.client.bonfire.BonfireSubscreenTransition;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** One authorized upgrade page, two independent purchases. Numbers come from the server snapshot. */
public final class FlaskScreen extends Screen {
    private static final int PANEL_WIDTH = 500, PANEL_HEIGHT = 342;
    private static final int TEXT = 0xFFE5D9C4, GOLD = 0xFFE2BD78, MUTED = 0xFFB5A78F;
    private FlaskPayloads.Menu view;
    private int red;
    private boolean pending, succeeded;
    private FlaskActionButton capacity, potency, confirm, less, more;
    private float scale, originX, originY;
    private final BonfireSubscreenTransition transition = new BonfireSubscreenTransition();
    private boolean sessionClosed;

    public FlaskScreen(FlaskPayloads.Menu view) {
        super(label(view.page() == FlaskPayloads.Page.UPGRADE ? "upgrade" : "allocation"));
        this.view = view;
        red = view.state().crimsonAllocated();
    }

    /** A late response must not reopen a closed page or replace a newer authorization. */
    public boolean accepts(FlaskPayloads.Menu updated) {
        return pending && !transition.exiting() && view.nonce().equals(updated.replyTo()) && view.page() == updated.page();
    }

    public void accept(FlaskPayloads.Menu updated) {
        view = updated;
        red = updated.state().crimsonAllocated();
        pending = false;
        succeeded = updated.result() == FlaskPayloads.Result.OK;
        refresh();
        if (succeeded && view.page() == FlaskPayloads.Page.ALLOCATION) onClose();
    }

    @Override protected void init() {
        scale = Math.max(0.1F, Math.min(1, Math.min((width - 16F) / PANEL_WIDTH, (height - 16F) / PANEL_HEIGHT)));
        originX = (width - PANEL_WIDTH * scale) / 2;
        originY = (height - PANEL_HEIGHT * scale) / 2;
        capacity = potency = confirm = less = more = null;
        if (view.page() == FlaskPayloads.Page.UPGRADE) {
            capacity = button(20, 266, 212, "capacity_action", () -> submit(FlaskPayloads.Action.UPGRADE_CAPACITY));
            potency = button(268, 266, 212, "potency_action", () -> submit(FlaskPayloads.Action.UPGRADE_POTENCY));
        } else {
            more = button(215, 172, 28, "toward_crimson", () -> { red++; refresh(); });
            less = button(257, 172, 28, "toward_ashen", () -> { red--; refresh(); });
            confirm = button(150, 270, 200, "confirm", () -> submit(FlaskPayloads.Action.ALLOCATE));
        }
        if (view.page() == FlaskPayloads.Page.UPGRADE) button(196, 315, 108, "back", this::onClose);
        refresh();
    }

    private FlaskActionButton button(int x, int y, int w, String key, Runnable action) {
        return addRenderableWidget(new FlaskActionButton(x, y, w, label(key), action));
    }

    private void submit(FlaskPayloads.Action action) {
        if (pending || !transition.interactive()) return;
        pending = true;
        succeeded = false;
        refresh();
        PacketDistributor.sendToServer(new FlaskPayloads.Request(view.nonce(), action,
                action == FlaskPayloads.Action.ALLOCATE ? red : 0,
                action == FlaskPayloads.Action.ALLOCATE ? view.state().totalCapacity() - red : 0));
    }

    private String disabledReason(boolean forCapacity) {
        if (view.result() == FlaskPayloads.Result.INVALID_SESSION) return "invalid_session";
        if (pending) return "pending";
        if (forCapacity ? view.state().totalCapacity() >= FlaskRules.MAX_CAPACITY
                : view.state().potencyLevel() >= FlaskRules.MAX_POTENCY) return "at_cap";
        if (forCapacity ? view.shards() < FlaskRules.capacityCost(view.state().totalCapacity()) : view.ash() < 1)
            return "insufficient_material";
        return null;
    }

    private void refresh() {
        if (capacity != null) capacity.active = disabledReason(true) == null;
        if (potency != null) potency.active = disabledReason(false) == null;
        boolean valid = !pending && view.result() != FlaskPayloads.Result.INVALID_SESSION;
        if (confirm != null) confirm.active = valid;
        if (less != null) less.active = valid && red > 0;
        if (more != null) more.active = valid && red < view.state().totalCapacity();
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        transition.render(g, () -> renderPage(g, transition.interactive() ? mouseX : -10000,
                transition.interactive() ? mouseY : -10000, partial));
    }

    private void renderPage(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int mx = (int) localX(mouseX), my = (int) localY(mouseY);
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(scale, scale, 1);
        panel(g, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xEC100E0B);
        g.drawCenteredString(font, title, 250, 9, GOLD);
        g.drawCenteredString(font, label(view.page() == FlaskPayloads.Page.UPGRADE
                ? "upgrade_subtitle" : "allocation_subtitle"), 250, 27, MUTED);
        if (view.page() == FlaskPayloads.Page.UPGRADE) {
            summary(g);
            upgradeCard(g, 8, true);
            upgradeCard(g, 256, false);
        } else {
            allocation(g);
        }
        String status = pending ? "pending" : succeeded ? "success" : view.result().name().toLowerCase(java.util.Locale.ROOT);
        if (capacity != null && hoveredOrFocused(capacity, mx, my) && disabledReason(true) != null) status = disabledReason(true);
        if (potency != null && hoveredOrFocused(potency, mx, my) && disabledReason(false) != null) status = disabledReason(false);
        if (!status.equals("ok"))
            g.drawCenteredString(font, Component.translatable("message.maplesadventure.flask." + status), 250, 298, MUTED);
        super.render(g, mx, my, partial);
        g.pose().popPose();
    }

    private void summary(GuiGraphics g) {
        var s = view.state();
        panel(g, 8, 45, 484, 86, 0xB21C1811);
        FlaskVisuals.glow(g, FlaskKind.CRIMSON, 57, 91, 94, true);
        FlaskVisuals.model(g, FlaskKind.CRIMSON, 1, 57, 93, 65);
        g.drawCenteredString(font, Component.translatable("item.maplesadventure.crimson_flask")
                .append(s.potencyLevel() == 0 ? "" : " +" + s.potencyLevel()), 250, 57, GOLD);
        g.drawCenteredString(font, label("total", s.totalCapacity()), 250, 77, TEXT);
        g.drawCenteredString(font, label("current_health", FlaskRules.health(s.potencyLevel())), 250, 94, TEXT);
        if (view.mana()) g.drawCenteredString(font, label("current_mana", FlaskRules.mana(s.potencyLevel())), 250, 111, TEXT);
        if (view.mana()) {
            FlaskVisuals.glow(g, FlaskKind.ASHEN, 446, 91, 88, true);
            FlaskVisuals.model(g, FlaskKind.ASHEN, 1, 446, 93, 65);
        }
    }

    private void allocation(GuiGraphics g) {
        panel(g, 8, 45, 484, 215, 0xB2121519);
        g.fill(249, 60, 250, 251, 0x666C604A);
        g.drawCenteredString(font, label("total", view.state().totalCapacity()), 250, 53, TEXT);
        allocationHalf(g, FlaskKind.CRIMSON, 124, red);
        allocationHalf(g, FlaskKind.ASHEN, 376, view.state().totalCapacity() - red);
    }

    private void allocationHalf(GuiGraphics g, FlaskKind kind, int cx, int count) {
        boolean crimson = kind == FlaskKind.CRIMSON;
        int color = crimson ? GOLD : 0xFFA5D8FF;
        FlaskVisuals.glow(g, kind, cx, 128, 154, count > 0);
        FlaskVisuals.model(g, kind, count > 0 ? 1 : 7, cx, 137, 100);
        g.drawCenteredString(font, Component.translatable("item.maplesadventure." + (crimson ? "crimson_flask" : "ashen_flask")), cx, 185, color);
        g.drawCenteredString(font, label(crimson ? "restores_health" : "restores_mana"), cx, 200, MUTED);
        g.pose().pushPose();
        g.pose().translate(cx, 216, 0);
        g.pose().scale(1.65F, 1.65F, 1);
        g.drawCenteredString(font, Integer.toString(count), 0, 0, color);
        g.pose().popPose();
        int total = view.state().totalCapacity();
        for (int i = 0; i < total; i++)
            FlaskVisuals.marker(g, kind, i < count, cx - total * 11 / 2 + i * 11, 238, 16);
    }

    private void upgradeCard(GuiGraphics g, int x, boolean forCapacity) {
        var s = view.state();
        boolean capped = forCapacity ? s.totalCapacity() >= FlaskRules.MAX_CAPACITY : s.potencyLevel() >= FlaskRules.MAX_POTENCY;
        int next = Math.min(FlaskRules.MAX_POTENCY, s.potencyLevel() + 1);
        panel(g, x, 142, 236, 150, 0xB2181510);
        g.drawString(font, label(forCapacity ? "capacity_action" : "potency_action"), x + 12, 152, GOLD, false);
        g.drawString(font, label(forCapacity ? "capacity_description" : "potency_description"), x + 12, 168, MUTED, false);
        g.fill(x + 12, 182, x + 224, 183, 0x775E4D33);
        Component preview = forCapacity ? label("capacity_preview", s.totalCapacity(), Math.min(FlaskRules.MAX_CAPACITY, s.totalCapacity() + 1))
                : label("health_preview", FlaskRules.health(s.potencyLevel()), FlaskRules.health(next));
        g.drawString(font, preview, x + 12, 193, TEXT, false);
        Component secondary = forCapacity ? label("capacity_limit", FlaskRules.MAX_CAPACITY)
                : view.mana() ? label("mana_preview", FlaskRules.mana(s.potencyLevel()), FlaskRules.mana(next))
                : label("potency_preview", s.potencyLevel(), next);
        g.drawString(font, secondary, x + 12, 210, !forCapacity && view.mana() ? TEXT : MUTED, false);
        texture(g, forCapacity ? "estus_shard" : "noble_ash", x + 12, 232, 24);
        g.drawString(font, Component.translatable("item.maplesadventure." + (forCapacity ? "estus_shard" : "noble_ash")), x + 44, 231, TEXT, false);
        int cost = capped ? 0 : forCapacity ? FlaskRules.capacityCost(s.totalCapacity()) : 1;
        g.drawString(font, capped ? Component.translatable("message.maplesadventure.flask.at_cap")
                : label("materials", forCapacity ? view.shards() : view.ash(), cost), x + 44, 247, MUTED, false);
    }

    private void texture(GuiGraphics g, String name, int x, int y, int size) {
        g.blit(ResourceLocation.parse("maplesadventure:textures/item/flask/" + name + ".png"),
                x, y, size, size, 0, 0, 32, 32, 32, 32);
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
        g.renderOutline(x, y, w, h, 0xBB94723D);
        g.fill(x + 5, y + 4, x + 28, y + 5, 0xFFCCAC6F);
        g.fill(x + w - 28, y + h - 5, x + w - 5, y + h - 4, 0xFFCCAC6F);
    }

    private static Component label(String key, Object... args) { return Component.translatable("screen.maplesadventure.flask." + key, args); }
    private static boolean hoveredOrFocused(AbstractWidget widget, int x, int y) {
        // Disabled widgets still need a visible reason; their isMouseOver may require active=true.
        return widget.isFocused() || (x >= widget.getX() && x < widget.getRight()
                && y >= widget.getY() && y < widget.getBottom());
    }
    private double localX(double x) { return (x - originX) / scale; }
    private double localY(double y) { return (y - originY) / scale; }
    @Override public boolean mouseClicked(double x, double y, int button) { return !transition.interactive() || super.mouseClicked(localX(x), localY(y), button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return !transition.interactive() || super.mouseReleased(localX(x), localY(y), button); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!transition.interactive()) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void mouseMoved(double x, double y) {
        if (!transition.interactive()) return;
        double lx = localX(x), ly = localY(y);
        super.mouseMoved(lx, ly);
        setFocused(children().stream().filter(c -> c instanceof AbstractWidget w && w.active && w.isMouseOver(lx, ly))
                .findFirst().orElse(null));
    }
    @Override public void tick() {
        if (!BonfireClient.resting()) { minecraft.setScreen(null); return; }
        transition.tick();
    }
    @Override public void onClose() {
        if (pending) return;
        transition.exit(() -> minecraft.setScreen(BonfireClient.resting() ? new BonfireScreen(BonfireClient.currentView()) : null));
    }
    @Override public void removed() {
        transition.close();
        if (!sessionClosed && minecraft.getConnection() != null) { sessionClosed = true; closeSession(view.nonce()); }
    }
    public static void closeSession(UUID nonce) { PacketDistributor.sendToServer(new FlaskPayloads.Request(nonce, FlaskPayloads.Action.CLOSE, 0, 0)); }
    @Override public boolean isPauseScreen() { return false; }

    private final class FlaskActionButton extends AbstractButton {
        private final Runnable action;
        private FlaskActionButton(int x, int y, int width, Component label, Runnable action) {
            super(x, y, width, 20, label);
            this.action = action;
        }
        @Override public void onPress() { if (active && transition.interactive()) action.run(); }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            panel(g, getX(), getY(), getWidth(), getHeight(), active && isFocused() ? 0xD9604925 : 0xD9201B13);
            g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 6, active ? GOLD : 0xFF827969);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
