package dev.maplesadventure.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Language-neutral phrase persisted and sent over the network. */
public final class MessagePhrase {
    private final String templateId;
    private final Map<String, String> slots;
    private final Map<String, List<String>> modifiers;

    public MessagePhrase(String templateId, Map<String, String> slots, Map<String, List<String>> modifiers) {
        this.templateId = templateId == null ? "" : templateId;
        this.slots = Map.copyOf(slots == null ? Map.of() : slots);
        LinkedHashMap<String, List<String>> copiedModifiers = new LinkedHashMap<>();
        if (modifiers != null) {
            modifiers.forEach((slot, values) -> copiedModifiers.put(slot, List.copyOf(values)));
        }
        this.modifiers = Map.copyOf(copiedModifiers);
    }

    /** Compatibility constructor for v1 records/network order. */
    public MessagePhrase(String templateId, List<String> orderedTokens) {
        this.templateId = templateId == null ? "" : templateId;
        LinkedHashMap<String, String> mapped = new LinkedHashMap<>();
        MessageTemplateRegistry.find(this.templateId).ifPresent(template -> {
            List<String> safeTokens = orderedTokens == null ? List.of() : orderedTokens;
            for (int index = 0; index < Math.min(template.slots().size(), safeTokens.size()); index++) {
                mapped.put(template.slots().get(index).id(), safeTokens.get(index));
            }
            for (int index = template.slots().size(); index < safeTokens.size(); index++) {
                mapped.put("__legacy_extra_" + index, safeTokens.get(index));
            }
        });
        this.slots = Map.copyOf(mapped);
        this.modifiers = Map.of();
    }

    public static MessagePhrase empty(String templateId) {
        return new MessagePhrase(templateId, Map.of(), Map.of());
    }

    public String templateId() { return templateId; }
    public Map<String, String> slots() { return slots; }
    public Map<String, List<String>> modifiers() { return modifiers; }

    /** Compatibility view in schema order for old diagnostics/tests. */
    public List<String> tokens() {
        MessageTemplate template = MessageTemplateRegistry.find(templateId).orElse(null);
        if (template == null) return List.copyOf(slots.values());
        ArrayList<String> result = new ArrayList<>(template.slots().size());
        for (MessageSlotDefinition definition : template.slots()) {
            String token = slots.get(definition.id());
            if (token != null) result.add(token);
        }
        return List.copyOf(result);
    }

    public MessagePhrase withSlot(String slotId, String tokenId) {
        LinkedHashMap<String, String> changed = new LinkedHashMap<>(slots);
        if (tokenId == null || tokenId.isBlank()) changed.remove(slotId);
        else changed.put(slotId, tokenId);
        return new MessagePhrase(templateId, changed, modifiers);
    }

    public MessagePhrase withModifier(String slotId, MessageModifierType type, String tokenId) {
        MessageTemplate template = MessageTemplateRegistry.find(templateId).orElse(null);
        if (template == null) return this;
        MessageSlotDefinition slot = template.slot(slotId);
        if (slot == null || !slot.optionalModifiers().contains(type)) return this;

        LinkedHashMap<String, List<String>> changed = new LinkedHashMap<>(modifiers);
        ArrayList<String> values = new ArrayList<>(changed.getOrDefault(slotId, List.of()));
        values.removeIf(id -> MessageTokenRegistry.find(id)
                .map(token -> token.modifierType() == type).orElse(false));
        if (tokenId != null && !tokenId.isBlank()) values.add(tokenId);
        if (values.isEmpty()) changed.remove(slotId);
        else changed.put(slotId, List.copyOf(values));
        return new MessagePhrase(templateId, slots, changed);
    }

    public String modifier(String slotId, MessageModifierType type) {
        for (String id : modifiers.getOrDefault(slotId, List.of())) {
            MessageToken token = MessageTokenRegistry.find(id).orElse(null);
            if (token != null && token.modifierType() == type) return id;
        }
        return null;
    }

    public boolean isComplete() {
        MessageTemplate template = MessageTemplateRegistry.find(templateId).orElse(null);
        if (template == null) return false;
        for (MessageSlotDefinition slot : template.slots()) {
            if (!slots.containsKey(slot.id())) return false;
        }
        return true;
    }
}
