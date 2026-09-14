package dev.maplesadventure.progression;

import java.util.EnumMap;
import java.util.Map;

/** Preview only. Never holds a player, connection, or attachment reference. */
public final class LevelUpDraft {
    private final EnumMap<Attribute, Integer> deltas = new EnumMap<>(Attribute.class);
    public int get(Attribute attribute) { return deltas.getOrDefault(attribute, 0); }
    public Map<Attribute, Integer> deltas() { return Map.copyOf(deltas); }
    public void clear() { deltas.clear(); }
    public void adjust(Attribute attribute, int amount, PlayerAttributeState baseline, int cap) {
        int value = Math.clamp((long) get(attribute) + amount, 0, Math.max(0, cap - baseline.get(attribute)));
        if (value == 0) deltas.remove(attribute);
        else deltas.put(attribute, value);
    }
}

