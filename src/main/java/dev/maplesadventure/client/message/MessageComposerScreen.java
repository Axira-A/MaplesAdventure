package dev.maplesadventure.client.message;

import dev.maplesadventure.message.MessageComponents;
import dev.maplesadventure.message.MessageConnector;
import dev.maplesadventure.message.MessageConnectorRegistry;
import dev.maplesadventure.message.MessageModifierType;
import dev.maplesadventure.message.MessagePhrase;
import dev.maplesadventure.message.MessageSlotDefinition;
import dev.maplesadventure.message.MessageTemplate;
import dev.maplesadventure.message.MessageTemplateRegistry;
import dev.maplesadventure.message.MessageToken;
import dev.maplesadventure.message.MessageTokenRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Two-phrase, schema-driven composer. It deliberately has no free-text input path. */
public final class MessageComposerScreen extends Screen {
    private MessagePhrase first = MessagePhrase.empty("location_has_object");
    private MessagePhrase second;
    private String connectorId = "therefore";
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int previewTop;
    private int connectorLabelY = -1;
    private int secondPhraseLabelY = -1;
    private String requestedFocus;
    private final Map<String, AbstractWidget> focusWidgets = new HashMap<>();

    public MessageComposerScreen() {
        super(Component.translatable("screen.maplesadventure.message_composer"));
    }

    @Override
    protected void init() {
        focusWidgets.clear();
        connectorLabelY = -1;
        secondPhraseLabelY = -1;
        panelWidth = Math.min(620, Math.max(390, (int)(width * 0.64F)));
        panelHeight = Math.min(410, Math.max(330, (int)(height * 0.74F)));
        panelWidth = Math.min(panelWidth, width - 24);
        panelHeight = Math.min(panelHeight, height - 18);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int contentLeft = panelLeft + 24;
        int contentWidth = panelWidth - 48;
        int y = panelTop + 36;
        y = addPhraseWidgets(0, first, contentLeft, contentWidth, y);

        if (second == null) {
            MessageSlotButton add = addRenderableWidget(new MessageSlotButton(contentLeft, y + 4, 150,
                    Component.translatable("screen.maplesadventure.message.add_phrase"), () -> {
                second = MessagePhrase.empty("subject_is_effective");
                requestedFocus = "connector";
                rebuildWidgets();
            }));
            add.setTooltip(Tooltip.create(Component.translatable("screen.maplesadventure.message.add_phrase_tooltip")));
            y += 29;
        } else {
            connectorLabelY = y + 8;
            MessageConnector connector = MessageConnectorRegistry.find(connectorId).orElseThrow();
            MessageSlotButton connectorButton = addRenderableWidget(new MessageSlotButton(contentLeft + 72, y + 3,
                    Math.min(150, contentWidth - 126), slotLabel("screen.maplesadventure.message.connector",
                    Component.translatable(connector.nameKey())), () -> minecraft.setScreen(
                    MessageSelectionScreens.connectors(this, connector, selected -> {
                        connectorId = selected.id();
                        requestedFocus = "template:1";
                    }))));
            connectorButton.setTooltip(Tooltip.create(Component.translatable("screen.maplesadventure.message.connector_description")));
            focusWidgets.put("connector", connectorButton);
            addRenderableWidget(new MessageSlotButton(contentLeft + contentWidth - 96, y + 3, 96,
                    Component.translatable("screen.maplesadventure.message.remove_phrase"), () -> {
                second = null;
                rebuildWidgets();
            }));
            y += 28;
            secondPhraseLabelY = y + 5;
            y = addPhraseWidgets(1, second, contentLeft, contentWidth, y);
        }

        previewTop = Math.max(y + 5, panelTop + panelHeight - 92);
        int buttonY = panelTop + panelHeight - 28;
        Button leave = addRenderableWidget(Button.builder(Component.translatable("screen.maplesadventure.message.leave"), button -> {
            List<MessagePhrase> phrases = second == null ? List.of(first) : List.of(first, second);
            List<String> connectors = second == null ? List.of() : List.of(connectorId);
            MessageClientActions.create(phrases, connectors);
            onClose();
        }).bounds(panelLeft + panelWidth / 2 - 122, buttonY, 116, 20).build());
        leave.active = complete();
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(panelLeft + panelWidth / 2 + 6, buttonY, 116, 20).build());

        AbstractWidget focus = focusWidgets.get(requestedFocus);
        if (focus != null) setInitialFocus(focus);
        requestedFocus = null;
    }

    private int addPhraseWidgets(int phraseIndex, MessagePhrase phrase, int left, int availableWidth, int top) {
        MessageTemplate template = MessageTemplateRegistry.find(phrase.templateId()).orElseThrow();
        int templateWidth = Math.min(230, availableWidth - 85);
        MessageSlotButton templateButton = addRenderableWidget(new MessageSlotButton(left + 76, top, templateWidth,
                slotLabel("screen.maplesadventure.message.template_short", Component.translatable(template.nameKey())),
                () -> minecraft.setScreen(MessageSelectionScreens.templates(this, template, selected -> {
                    setPhrase(phraseIndex, MessagePhrase.empty(selected.id()));
                    requestedFocus = firstSlotFocus(phraseIndex, selected);
                }))));
        templateButton.setTooltip(Tooltip.create(Component.translatable(template.exampleKey())));
        focusWidgets.put("template:" + phraseIndex, templateButton);
        int y = top + 24;
        int x = left;
        for (int slotIndex = 0; slotIndex < template.slots().size(); slotIndex++) {
            MessageSlotDefinition slot = template.slots().get(slotIndex);
            String tokenId = phrase.slots().get(slot.id());
            MessageToken selectedToken = tokenId == null ? null : MessageTokenRegistry.find(tokenId).orElse(null);
            Component value = selectedToken == null
                    ? Component.translatable("screen.maplesadventure.message.unset")
                    : Component.translatable(selectedToken.translationKey());
            Component label = slotLabel(slot.labelKey(), value);
            int chipWidth = Math.max(86, Math.min(150, font.width(label) + 16));
            if (x + chipWidth > left + availableWidth) { x = left; y += 22; }
            int currentSlotIndex = slotIndex;
            MessageSlotButton tokenButton = addRenderableWidget(new MessageSlotButton(x, y, chipWidth, label,
                    () -> minecraft.setScreen(MessageSelectionScreens.tokens(this, slot, selectedToken, selected -> {
                        MessagePhrase changed = getPhrase(phraseIndex).withSlot(slot.id(), selected.id());
                        setPhrase(phraseIndex, changed);
                        requestedFocus = nextIncompleteFocus(phraseIndex, currentSlotIndex, template);
                    }))));
            tokenButton.setTooltip(Tooltip.create(Component.translatable(slot.descriptionKey())));
            focusWidgets.put("slot:" + phraseIndex + ":" + slot.id(), tokenButton);
            x += chipWidth + 6;

            if (!slot.optionalModifiers().isEmpty()) {
                MessageToken modifier = selectedModifier(phrase, slot);
                Component modifierValue = modifier == null
                        ? Component.translatable("screen.maplesadventure.message.none")
                        : Component.translatable(modifier.translationKey());
                Component modifierLabel = slotLabel("screen.maplesadventure.message.adjective", modifierValue);
                int modifierWidth = Math.max(90, Math.min(145, font.width(modifierLabel) + 16));
                if (x + modifierWidth > left + availableWidth) { x = left; y += 22; }
                MessageSlotButton modifierButton = addRenderableWidget(new MessageSlotButton(x, y, modifierWidth, modifierLabel,
                        () -> openModifierBrowser(phraseIndex, slot, modifier)));
                modifierButton.setTooltip(Tooltip.create(Component.translatable("screen.maplesadventure.message.modifier_description")));
                focusWidgets.put("modifier:" + phraseIndex + ":" + slot.id(), modifierButton);
                x += modifierWidth + 6;
            }
        }
        return y + 24;
    }

    private void openModifierBrowser(int phraseIndex, MessageSlotDefinition slot, MessageToken current) {
        List<MessageToken> choices = new ArrayList<>();
        for (MessageModifierType type : slot.optionalModifiers()) choices.addAll(MessageTokenRegistry.modifiers(type));
        List<SearchableSelectionScreen.SelectionItem<MessageToken>> items = new ArrayList<>();
        items.add(new SearchableSelectionScreen.SelectionItem<>(null, "all",
                Component.translatable("screen.maplesadventure.message.category.all"),
                Component.translatable("screen.maplesadventure.message.none"),
                Component.translatable("screen.maplesadventure.message.modifier_none_description"), List.of("none")));
        for (MessageToken token : choices) {
            items.add(new SearchableSelectionScreen.SelectionItem<>(token, token.modifierType().name(),
                    Component.translatable("screen.maplesadventure.message.category.modifier."
                            + token.modifierType().name().toLowerCase(java.util.Locale.ROOT)),
                    Component.translatable(token.translationKey()),
                    Component.translatable("screen.maplesadventure.message.modifier_description"), List.of(token.id())));
        }
        minecraft.setScreen(new SearchableSelectionScreen<>(this,
                Component.translatable("screen.maplesadventure.message.select_modifier"), items, current, selected -> {
            MessagePhrase changed = getPhrase(phraseIndex);
            for (MessageModifierType type : slot.optionalModifiers()) changed = changed.withModifier(slot.id(), type, null);
            if (selected != null) changed = changed.withModifier(slot.id(), selected.modifierType(), selected.id());
            setPhrase(phraseIndex, changed);
        }));
    }

    private MessageToken selectedModifier(MessagePhrase phrase, MessageSlotDefinition slot) {
        for (String id : phrase.modifiers().getOrDefault(slot.id(), List.of())) {
            MessageToken token = MessageTokenRegistry.find(id).orElse(null);
            if (token != null) return token;
        }
        return null;
    }

    private Component slotLabel(String labelKey, Component value) {
        return Component.translatable("screen.maplesadventure.message.slot_chip",
                Component.translatable(labelKey), value);
    }

    private String firstSlotFocus(int phraseIndex, MessageTemplate template) {
        return template.slots().isEmpty() ? "template:" + phraseIndex
                : "slot:" + phraseIndex + ":" + template.slots().getFirst().id();
    }

    private String nextIncompleteFocus(int phraseIndex, int currentIndex, MessageTemplate template) {
        MessagePhrase phrase = getPhrase(phraseIndex);
        for (int index = currentIndex + 1; index < template.slots().size(); index++) {
            MessageSlotDefinition slot = template.slots().get(index);
            if (!phrase.slots().containsKey(slot.id())) return "slot:" + phraseIndex + ":" + slot.id();
        }
        return "template:" + phraseIndex;
    }

    private MessagePhrase getPhrase(int index) { return index == 0 ? first : second; }
    private void setPhrase(int index, MessagePhrase phrase) { if (index == 0) first = phrase; else second = phrase; }
    private boolean complete() {
        return first.isComplete() && (second == null || (second.isComplete() && MessageConnectorRegistry.find(connectorId).isPresent()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE0100D09);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0xFF9E743C);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 11, 0xFFE8CA8D);
        graphics.drawString(font, Component.translatable("screen.maplesadventure.message.phrase_one"),
                panelLeft + 24, panelTop + 41, 0xFFAFA18B, false);
        if (connectorLabelY >= 0) {
            graphics.drawString(font, Component.translatable("screen.maplesadventure.message.connector_label"),
                    panelLeft + 24, connectorLabelY, 0xFFAFA18B, false);
        }
        if (secondPhraseLabelY >= 0) {
            graphics.drawString(font, Component.translatable("screen.maplesadventure.message.phrase_two"),
                    panelLeft + 24, secondPhraseLabelY, 0xFFAFA18B, false);
        }
        renderPreview(graphics);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics) {
        int previewLeft = panelLeft + 24;
        int previewWidth = panelWidth - 48;
        int maxBottom = panelTop + panelHeight - 34;
        int top = Math.min(previewTop, maxBottom - 42);
        graphics.fill(previewLeft, top, previewLeft + previewWidth, maxBottom, 0xA00B0907);
        graphics.renderOutline(previewLeft, top, previewWidth, maxBottom - top, 0xFF624A2E);
        graphics.drawCenteredString(font, Component.translatable("screen.maplesadventure.message.preview"),
                width / 2, top + 6, 0xFF9F9587);
        Component preview = complete()
                ? MessageComponents.compose(second == null ? List.of(first) : List.of(first, second),
                        second == null ? List.of() : List.of(connectorId))
                : MessageComponents.incompletePreview();
        List<FormattedCharSequence> lines = font.split(preview, previewWidth - 24);
        int y = top + 20;
        for (int index = 0; index < Math.min(3, lines.size()); index++) {
            graphics.drawCenteredString(font, lines.get(index), width / 2, y, complete() ? 0xFFFFD58A : 0xFF9F9587);
            y += 10;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
