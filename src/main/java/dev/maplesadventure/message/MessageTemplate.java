package dev.maplesadventure.message;

import java.util.List;

/** Schema for one phrase. Slot IDs, rather than ordinal UI fields, are persisted. */
public record MessageTemplate(
        String id,
        String translationKey,
        String modifiedTranslationKey,
        String nameKey,
        String exampleKey,
        MessageTemplateCategory category,
        List<MessageSlotDefinition> slots
) {
    public MessageTemplate {
        slots = List.copyOf(slots);
    }

    public MessageSlotDefinition slot(String slotId) {
        for (MessageSlotDefinition slot : slots) {
            if (slot.id().equals(slotId)) return slot;
        }
        return null;
    }
}
