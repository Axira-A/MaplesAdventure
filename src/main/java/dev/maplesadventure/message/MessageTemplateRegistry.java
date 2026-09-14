package dev.maplesadventure.message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Defines legal phrase shapes; exact slot and modifier matching is the server allow-list. */
public final class MessageTemplateRegistry {
    private static final Map<String, MessageTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        register("location_has_object", MessageTemplateCategory.LOCATION, List.of(
                slot("location", MessageSlotType.LOCATION),
                objectSlot("object")
        ));
        register("beware_of", MessageTemplateCategory.WARNING, List.of(objectSlot("object")));
        register("try_action", MessageTemplateCategory.ACTION, List.of(slot("action", MessageSlotType.ACTION)));
        register("location_requires_action", MessageTemplateCategory.ADVICE, List.of(
                slot("location", MessageSlotType.LOCATION), slot("action", MessageSlotType.ACTION)
        ));
        register("praise_object", MessageTemplateCategory.ENCOURAGEMENT, List.of(objectSlot("object")));
        register("simple_phrase", MessageTemplateCategory.ENCOURAGEMENT,
                List.of(slot("phrase", MessageSlotType.SIMPLE_PHRASE)));
        register("subject_is_effective", MessageTemplateCategory.EVALUATION,
                List.of(objectSlot("subject", MessageSlotType.SUBJECT)));
        register("subject_is_useless", MessageTemplateCategory.EVALUATION,
                List.of(objectSlot("subject", MessageSlotType.SUBJECT)));
    }

    public static Optional<MessageTemplate> find(String id) { return Optional.ofNullable(TEMPLATES.get(id)); }
    public static List<MessageTemplate> all() { return List.copyOf(TEMPLATES.values()); }

    public static boolean isValid(MessagePhrase phrase) {
        if (phrase == null || phrase.templateId().length() > 32 || phrase.slots().size() > 4
                || phrase.modifiers().size() > 4) return false;
        MessageTemplate template = TEMPLATES.get(phrase.templateId());
        if (template == null || template.slots().size() != phrase.slots().size()) return false;

        for (MessageSlotDefinition slot : template.slots()) {
            String tokenId = phrase.slots().get(slot.id());
            if (tokenId == null || tokenId.length() > 32) return false;
            MessageToken token = MessageTokenRegistry.find(tokenId).orElse(null);
            if (token == null || token.isModifier() || !token.allowedIn(slot.type())) return false;

            List<String> modifiers = phrase.modifiers().getOrDefault(slot.id(), List.of());
            if (modifiers.size() > slot.optionalModifiers().size()) return false;
            java.util.HashSet<MessageModifierType> seen = new java.util.HashSet<>();
            for (String modifierId : modifiers) {
                MessageToken modifier = MessageTokenRegistry.find(modifierId).orElse(null);
                if (modifier == null || !modifier.isModifier() || modifierId.length() > 32
                        || !slot.optionalModifiers().contains(modifier.modifierType())
                        || !seen.add(modifier.modifierType())) return false;
            }
        }
        for (String slotId : phrase.slots().keySet()) if (template.slot(slotId) == null) return false;
        for (String slotId : phrase.modifiers().keySet()) if (template.slot(slotId) == null) return false;
        return true;
    }

    private static MessageSlotDefinition slot(String id, MessageSlotType type) {
        return new MessageSlotDefinition(id, type,
                "screen.maplesadventure.message.slot." + type.name().toLowerCase(java.util.Locale.ROOT),
                "screen.maplesadventure.message.slot_description." + type.name().toLowerCase(java.util.Locale.ROOT),
                List.of());
    }

    private static MessageSlotDefinition objectSlot(String id) {
        return objectSlot(id, MessageSlotType.OBJECT);
    }

    private static MessageSlotDefinition objectSlot(String id, MessageSlotType type) {
        return new MessageSlotDefinition(id, type,
                "screen.maplesadventure.message.slot." + type.name().toLowerCase(java.util.Locale.ROOT),
                "screen.maplesadventure.message.slot_description." + type.name().toLowerCase(java.util.Locale.ROOT),
                List.of(MessageModifierType.ADJECTIVE_SIZE, MessageModifierType.ADJECTIVE_QUALITY,
                        MessageModifierType.ADJECTIVE_DANGER));
    }

    private static void register(String id, MessageTemplateCategory category, List<MessageSlotDefinition> slots) {
        MessageTemplate template = new MessageTemplate(id,
                "message.maplesadventure.template." + id,
                "message.maplesadventure.template." + id + ".modified",
                "message.maplesadventure.template_name." + id,
                "message.maplesadventure.template_example." + id,
                category, slots);
        if (TEMPLATES.putIfAbsent(id, template) != null) throw new IllegalStateException("Duplicate template: " + id);
    }

    private MessageTemplateRegistry() {}
}
