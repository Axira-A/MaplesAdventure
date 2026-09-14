package dev.maplesadventure.client.message;

import dev.maplesadventure.message.MessageConnector;
import dev.maplesadventure.message.MessageConnectorRegistry;
import dev.maplesadventure.message.MessageModifierType;
import dev.maplesadventure.message.MessageSlotDefinition;
import dev.maplesadventure.message.MessageTemplate;
import dev.maplesadventure.message.MessageTemplateRegistry;
import dev.maplesadventure.message.MessageToken;
import dev.maplesadventure.message.MessageTokenRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MessageSelectionScreens {
    static Screen templates(Screen parent, MessageTemplate current, Consumer<MessageTemplate> selected) {
        List<SearchableSelectionScreen.SelectionItem<MessageTemplate>> items = MessageTemplateRegistry.all().stream()
                .map(template -> new SearchableSelectionScreen.SelectionItem<>(template,
                        template.category().name(), category("template", template.category().name()),
                        Component.translatable(template.nameKey()), Component.translatable(template.exampleKey()),
                        List.of(template.id())))
                .toList();
        return new SearchableSelectionScreen<>(parent,
                Component.translatable("screen.maplesadventure.message.select_template"), items, current, selected);
    }

    static Screen tokens(Screen parent, MessageSlotDefinition slot, MessageToken current, Consumer<MessageToken> selected) {
        List<SearchableSelectionScreen.SelectionItem<MessageToken>> items = MessageTokenRegistry.tokensFor(slot.type()).stream()
                .map(token -> new SearchableSelectionScreen.SelectionItem<>(token,
                        token.category().name(), category("token", token.category().name()),
                        Component.translatable(token.translationKey()), Component.translatable(slot.descriptionKey()),
                        List.of(token.id())))
                .toList();
        return new SearchableSelectionScreen<>(parent,
                Component.translatable("screen.maplesadventure.message.select_slot",
                        Component.translatable(slot.labelKey())), items, current, selected);
    }

    static Screen modifiers(Screen parent, MessageModifierType type, MessageToken current, Consumer<MessageToken> selected) {
        ArrayList<SearchableSelectionScreen.SelectionItem<MessageToken>> items = new ArrayList<>();
        items.add(new SearchableSelectionScreen.SelectionItem<>(null, "all",
                Component.translatable("screen.maplesadventure.message.category.all"),
                Component.translatable("screen.maplesadventure.message.none"),
                Component.translatable("screen.maplesadventure.message.modifier_none_description"), List.of("none")));
        for (MessageToken token : MessageTokenRegistry.modifiers(type)) {
            items.add(new SearchableSelectionScreen.SelectionItem<>(token, type.name(),
                    category("modifier", type.name()), Component.translatable(token.translationKey()),
                    Component.translatable("screen.maplesadventure.message.modifier_description"), List.of(token.id())));
        }
        return new SearchableSelectionScreen<>(parent,
                Component.translatable("screen.maplesadventure.message.select_modifier"), items, current, selected);
    }

    static Screen connectors(Screen parent, MessageConnector current, Consumer<MessageConnector> selected) {
        List<SearchableSelectionScreen.SelectionItem<MessageConnector>> items = MessageConnectorRegistry.all().stream()
                .map(connector -> new SearchableSelectionScreen.SelectionItem<>(connector, "all",
                        Component.translatable("screen.maplesadventure.message.category.all"),
                        Component.translatable(connector.nameKey()),
                        Component.translatable("screen.maplesadventure.message.connector_description"),
                        List.of(connector.id())))
                .toList();
        return new SearchableSelectionScreen<>(parent,
                Component.translatable("screen.maplesadventure.message.select_connector"), items, current, selected);
    }

    private static Component category(String group, String name) {
        return Component.translatable("screen.maplesadventure.message.category." + group + "."
                + name.toLowerCase(Locale.ROOT));
    }

    private MessageSelectionScreens() {}
}
