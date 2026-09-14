package dev.maplesadventure.message;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Builds localized sentences from semantic IDs; persisted data never contains localized prose. */
public final class MessageComponents {
    public static Component compose(MessagePhrase phrase) {
        MessageTemplate template = MessageTemplateRegistry.find(phrase.templateId()).orElse(null);
        if (template == null || !MessageTemplateRegistry.isValid(phrase)) {
            return Component.translatable("message.maplesadventure.unreadable");
        }
        ArrayList<Object> arguments = new ArrayList<>();
        for (MessageSlotDefinition slot : template.slots()) {
            MessageToken token = MessageTokenRegistry.find(phrase.slots().get(slot.id())).orElseThrow();
            arguments.add(Component.translatable(token.translationKey()));
        }
        MessageToken modifier = firstModifier(phrase, template);
        if (modifier != null) arguments.add(Component.translatable(modifier.translationKey()));
        return Component.translatable(modifier == null ? template.translationKey() : template.modifiedTranslationKey(),
                arguments.toArray());
    }

    public static Component compose(List<MessagePhrase> phrases, List<String> connectors) {
        if (phrases == null || phrases.isEmpty()) return Component.translatable("message.maplesadventure.unreadable");
        Component first = compose(phrases.getFirst());
        if (phrases.size() == 1) {
            String sentenceKey = phrases.getFirst().templateId().equals("praise_object")
                    ? "message.maplesadventure.sentence.exclamation"
                    : "message.maplesadventure.sentence.single";
            return Component.translatable(sentenceKey, first);
        }
        String connectorId = connectors == null || connectors.isEmpty() ? "and" : connectors.getFirst();
        MessageConnector connector = MessageConnectorRegistry.find(connectorId)
                .orElseGet(() -> MessageConnectorRegistry.find("and").orElseThrow());
        return Component.translatable(connector.joinKey(), first, compose(phrases.get(1)));
    }

    public static Component incompletePreview() {
        return Component.translatable("screen.maplesadventure.message.complete_prompt");
    }

    private static MessageToken firstModifier(MessagePhrase phrase, MessageTemplate template) {
        for (MessageSlotDefinition slot : template.slots()) {
            for (String modifierId : phrase.modifiers().getOrDefault(slot.id(), List.of())) {
                MessageToken modifier = MessageTokenRegistry.find(modifierId).orElse(null);
                if (modifier != null && modifier.isModifier()) return modifier;
            }
        }
        return null;
    }

    private MessageComponents() {}
}
