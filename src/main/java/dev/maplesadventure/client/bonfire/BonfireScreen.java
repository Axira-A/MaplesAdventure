package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.bonfire.BonfireSessionState;
import dev.maplesadventure.bonfire.network.BonfirePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Deliberately plain placeholder menu; final bonfire art is supplied separately. */
public final class BonfireScreen extends Screen {
    private final BonfirePayloads.View view;
    public BonfireScreen(BonfirePayloads.View view) {
        super(Component.translatable("screen.maplesadventure.bonfire"));
        this.view = view;
    }
    @Override protected void init() {
        int x = width / 2 - 75;
        int y = height / 2 - 28;
        if (view.state() == BonfireSessionState.OPEN_STANDING)
            addRenderableWidget(Button.builder(Component.translatable("screen.maplesadventure.bonfire.rest"),
                    b -> BonfireClient.request(BonfirePayloads.ActionType.REST)).bounds(x, y, 150, 20).build());
        if (view.state() == BonfireSessionState.RESTING && view.canLevelUp())
            addRenderableWidget(Button.builder(Component.translatable("screen.maplesadventure.bonfire.level_up"),
                    b -> BonfireClient.request(BonfirePayloads.ActionType.LEVEL_UP)).bounds(x, y, 150, 20).build());
        if (view.state() == BonfireSessionState.OPEN_STANDING || view.state() == BonfireSessionState.RESTING)
            addRenderableWidget(Button.builder(Component.translatable("screen.maplesadventure.bonfire.leave"),
                    b -> BonfireClient.request(BonfirePayloads.ActionType.LEAVE)).bounds(x, y + 25, 150, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = width / 2 - 106, y = height / 2 - 65;
        graphics.fill(x, y, x + 212, y + 120, 0xD0191713);
        graphics.renderOutline(x, y, 212, 120, 0xFFAC8050);
        graphics.drawCenteredString(font, view.name().isBlank() ? Component.translatable("block.maplesadventure.bonfire")
                : Component.literal(view.name()), width / 2, y + 13, 0xFFE5D0A5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() {
        BonfireClient.request(BonfirePayloads.ActionType.LEAVE);
        // RESTING closes only after the authoritative stand-up interval; keep movement input in the UI meanwhile.
        if (view.state() == BonfireSessionState.OPEN_STANDING) super.onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
}
