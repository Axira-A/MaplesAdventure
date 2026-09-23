package dev.maplesadventure.bonfire;

import java.util.Arrays;
import java.util.Optional;

/** Stable wire/NBT names; enum ordinals are never persisted. */
public enum BonfireFeature {
    LEVEL_UP("level_up"), WARP("warp"), FLASK_ALLOCATION("flask_allocation"),
    SPELL_MEMORY("spell_memory"), REINFORCE("reinforce");

    private final String id;
    BonfireFeature(String id) { this.id = id; }
    public String id() { return id; }
    public static Optional<BonfireFeature> byId(String id) {
        return Arrays.stream(values()).filter(feature -> feature.id.equals(id)).findFirst();
    }
}
