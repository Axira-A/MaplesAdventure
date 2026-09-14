package dev.maplesadventure.multiplayer.hub.client;

import dev.maplesadventure.client.input.AdventureKeyMappings;
import dev.maplesadventure.client.message.MessageClientActions;
import dev.maplesadventure.multiplayer.hub.MultiplayerAction;
import dev.maplesadventure.multiplayer.hub.MultiplayerHubState;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact Souls-like entry point for active multiplayer intentions; world targets remain on F/Y. */
public final class MultiplayerScreen extends Screen {
    private MultiplayerHubState renderedState = MultiplayerHubState.empty();
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int refreshTicks;

    public MultiplayerScreen() { super(Component.translatable("screen.maplesadventure.multiplayer.title")); }

    @Override protected void init() {
        renderedState = MultiplayerHubClientState.get();
        panelWidth = Math.min(520, Math.max(340, (int) (width * 0.58F)));
        panelHeight = Math.min(310, Math.max(230, (int) (height * 0.62F)));
        panelWidth = Math.min(panelWidth, width - 24);
        panelHeight = Math.min(panelHeight, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        buildActions(renderedState);
        MultiplayerHubClientActions.requestState();
    }

    private void buildActions(MultiplayerHubState state) {
        int gap = 10;
        int buttonWidth = (panelWidth - 48 - gap) / 2;
        int buttonHeight = 38;
        int left = panelLeft + 24;
        int top = panelTop + 96;

        boolean messageEnabled = state.messageAvailable();
        addAction(left, top, buttonWidth, buttonHeight,
                Component.translatable("screen.maplesadventure.multiplayer.message"), messageEnabled,
                Component.translatable("screen.maplesadventure.multiplayer.message_hint"), reason(state, messageEnabled),
                () -> MessageClientActions.openComposer());

        boolean signEnabled = state.signAvailable();
        Component coopLabel = Component.translatable(state.coopSign()
                ? "screen.maplesadventure.multiplayer.coop_remove" : "screen.maplesadventure.multiplayer.coop_place");
        addAction(left + buttonWidth + gap, top, buttonWidth, buttonHeight, coopLabel, signEnabled,
                Component.translatable("screen.maplesadventure.multiplayer.coop_hint"), reason(state, signEnabled),
                () -> MultiplayerHubClientActions.send(state.coopSign()
                        ? MultiplayerAction.REMOVE_COOP_SIGN : MultiplayerAction.PLACE_COOP_SIGN));

        Component duelLabel = Component.translatable(state.duelSign()
                ? "screen.maplesadventure.multiplayer.duel_remove" : "screen.maplesadventure.multiplayer.duel_place");
        addAction(left, top + buttonHeight + gap, buttonWidth, buttonHeight, duelLabel, signEnabled,
                Component.translatable("screen.maplesadventure.multiplayer.duel_hint"), reason(state, signEnabled),
                () -> MultiplayerHubClientActions.send(state.duelSign()
                        ? MultiplayerAction.REMOVE_DUEL_SIGN : MultiplayerAction.PLACE_DUEL_SIGN));

        boolean invasionEnabled = state.invasionAvailable();
        Component invasionLabel = Component.translatable(state.invasionQueued()
                ? "screen.maplesadventure.multiplayer.invasion_cancel" : "screen.maplesadventure.multiplayer.invasion_seek");
        addAction(left + buttonWidth + gap, top + buttonHeight + gap, buttonWidth, buttonHeight,
                invasionLabel, invasionEnabled, Component.translatable("screen.maplesadventure.multiplayer.invasion_hint"),
                reason(state, invasionEnabled),
                () -> MultiplayerHubClientActions.send(state.invasionQueued()
                        ? MultiplayerAction.CANCEL_INVASION : MultiplayerAction.SEEK_INVASION));

        addRenderableWidget(Button.builder(Component.translatable("gui.close"), button -> onClose())
                .bounds(panelLeft + panelWidth / 2 - 55, panelTop + panelHeight - 32, 110, 20).build());
    }

    private void addAction(int x, int y, int width, int height, Component label, boolean enabled,
                           Component enabledDescription, Component reason, Runnable action) {
        Button button = addRenderableWidget(Button.builder(label, ignored -> action.run()).bounds(x, y, width, height).build());
        button.active = enabled;
        button.setTooltip(Tooltip.create(enabled ? enabledDescription : reason));
    }

    private static Component reason(MultiplayerHubState state, boolean enabled) {
        if (enabled) return Component.empty();
        if (state.bossActive()) return Component.translatable("screen.maplesadventure.multiplayer.disabled.boss");
        if (state.role() == PhaseRole.COOPERATOR) return Component.translatable("screen.maplesadventure.multiplayer.disabled.cooperator");
        if (state.role() == PhaseRole.INVADER) return Component.translatable("screen.maplesadventure.multiplayer.disabled.invader");
        if (state.coopSession() || state.hostileSession() || state.role() == PhaseRole.HOST)
            return Component.translatable("screen.maplesadventure.multiplayer.disabled.session");
        return Component.translatable("screen.maplesadventure.multiplayer.disabled.state");
    }

    @Override public void tick() {
        super.tick();
        if (++refreshTicks % 20 == 0) MultiplayerHubClientActions.requestState();
        MultiplayerHubState current = MultiplayerHubClientState.get();
        if (!current.equals(renderedState)) rebuildWidgets();
    }

    @Override protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AdventureKeyMappings.OPEN_MULTIPLAYER_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE3120E0A);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0xFF9E743C);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 15, 0xFFF0D29A);
        graphics.drawCenteredString(font, Component.translatable("screen.maplesadventure.multiplayer.phase",
                Component.translatable("screen.maplesadventure.multiplayer.role." + renderedState.role().name().toLowerCase())),
                width / 2, panelTop + 40, 0xFFD7C8AE);
        graphics.drawCenteredString(font, statusLine(renderedState), width / 2, panelTop + 57, 0xFFB9AA91);
        // Screen#render invokes renderBackground() itself. Calling it here after the
        // panel has been drawn would run a second blur pass over the panel and text,
        // leaving only the subsequently-rendered widgets sharp. Render the widgets
        // directly so the world is blurred exactly once, before every hub element.
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static Component statusLine(MultiplayerHubState state) {
        List<Component> details = new ArrayList<>();
        if (!state.cooperatorName().isBlank()) details.add(Component.translatable(
                "screen.maplesadventure.multiplayer.status.cooperator", state.cooperatorName()));
        if (!state.invaderName().isBlank()) details.add(Component.translatable(
                "screen.maplesadventure.multiplayer.status.invader", state.invaderName()));
        if (state.invasionQueued()) details.add(Component.translatable("screen.maplesadventure.multiplayer.status.seeking"));
        if (details.isEmpty()) return Component.translatable(state.coopSession() || state.hostileSession()
                ? "screen.maplesadventure.multiplayer.status.active" : "screen.maplesadventure.multiplayer.status.none");
        Component result = Component.empty();
        for (int index = 0; index < details.size(); index++) {
            if (index > 0) result = result.copy().append(Component.literal(" · "));
            result = result.copy().append(details.get(index));
        }
        return result;
    }

    @Override public boolean isPauseScreen() { return false; }
}
