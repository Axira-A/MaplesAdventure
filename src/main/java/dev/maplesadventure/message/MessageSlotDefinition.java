package dev.maplesadventure.message;

import java.util.List;

public record MessageSlotDefinition(
        String id,
        MessageSlotType type,
        String labelKey,
        String descriptionKey,
        List<MessageModifierType> optionalModifiers
) {
    public MessageSlotDefinition {
        optionalModifiers = List.copyOf(optionalModifiers);
    }

    public MessageSlotDefinition(String id, MessageSlotType type) {
        this(
                id,
                type,
                "message.maplesadventure.slot." + type.name().toLowerCase(java.util.Locale.ROOT),
                "message.maplesadventure.slot." + type.name().toLowerCase(java.util.Locale.ROOT) + ".description",
                List.of()
        );
    }
}
