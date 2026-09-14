package dev.maplesadventure.message;

import java.util.Set;
import javax.annotation.Nullable;

/** Closed vocabulary metadata. IDs are persisted; translated labels remain client-side. */
public record MessageToken(
        String id,
        MessageTokenCategory category,
        String translationKey,
        Set<MessageSlotType> allowedSlotTypes,
        @Nullable MessageModifierType modifierType,
        Set<String> tags
) {
    public MessageToken {
        allowedSlotTypes = Set.copyOf(allowedSlotTypes);
        tags = Set.copyOf(tags);
    }

    public boolean allowedIn(MessageSlotType slotType) {
        return allowedSlotTypes.contains(slotType);
    }

    public boolean isModifier() {
        return modifierType != null;
    }
}
