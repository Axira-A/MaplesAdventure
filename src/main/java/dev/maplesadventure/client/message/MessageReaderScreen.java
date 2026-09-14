package dev.maplesadventure.client.message;

import dev.maplesadventure.message.MessageComponents;
import dev.maplesadventure.message.MessageRating;
import dev.maplesadventure.message.MessageSummary;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class MessageReaderScreen extends Screen {
    private final MessageSummary message;
    private final MessageRating viewerRating;
    private final boolean ownMessage;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public MessageReaderScreen(MessageSummary message, MessageRating viewerRating, boolean ownMessage) {
        super(Component.translatable("screen.maplesadventure.message_reader"));
        this.message = message;
        this.viewerRating = viewerRating;
        this.ownMessage = ownMessage;
    }

    public UUID messageId() { return message.messageId(); }

    @Override
    protected void init() {
        panelWidth = Math.min(520, Math.max(320, (int)(width * 0.58F)));
        panelHeight = Math.min(270, Math.max(190, (int)(height * 0.46F)));
        panelWidth = Math.min(panelWidth, width - 24);
        panelHeight = Math.min(panelHeight, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        int y = panelTop + panelHeight - 55;
        int center = width / 2;
        if (ownMessage) {
            addRenderableWidget(Button.builder(Component.translatable("screen.maplesadventure.message.delete"), button -> {
                MessageClientActions.delete(message.messageId());
                onClose();
            }).bounds(center - 122, y, 116, 20).build());
        } else {
            addRenderableWidget(Button.builder(ratingLabel(MessageRating.POSITIVE), button ->
                    MessageClientActions.rate(message.messageId(), MessageRating.POSITIVE))
                    .bounds(center - 122, y, 116, 20).build());
            addRenderableWidget(Button.builder(ratingLabel(MessageRating.NEGATIVE), button ->
                    MessageClientActions.rate(message.messageId(), MessageRating.NEGATIVE))
                    .bounds(center + 6, y, 116, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.close"), button -> onClose())
                .bounds(center - 58, y + 27, 116, 20).build());
    }

    private Component ratingLabel(MessageRating rating) {
        String key = rating == MessageRating.POSITIVE
                ? "screen.maplesadventure.message.positive" : "screen.maplesadventure.message.negative";
        return viewerRating == rating
                ? Component.translatable("screen.maplesadventure.message.rating_selected", Component.translatable(key))
                : Component.translatable(key);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE0100D09);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0xFF9E743C);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 12, 0xFFE8CA8D);

        Component body = MessageComponents.compose(message.phrases(), message.connectors());
        List<FormattedCharSequence> lines = font.split(body, panelWidth - 48);
        int bodyY = panelTop + 39;
        for (int index = 0; index < Math.min(5, lines.size()); index++) {
            graphics.drawCenteredString(font, lines.get(index), width / 2, bodyY + index * 11, 0xFFFFD58A);
        }
        int ratingsY = Math.min(panelTop + 104, panelTop + panelHeight - 82);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.maplesadventure.message.ratings", message.positiveRatings(), message.negativeRatings()
        ), width / 2, ratingsY, 0xFFC7B58F);
        if (ownMessage) {
            graphics.drawCenteredString(font, Component.translatable("screen.maplesadventure.message.own"),
                    width / 2, ratingsY + 16, 0xFF9F9587);
        }
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
