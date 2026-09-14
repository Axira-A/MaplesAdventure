package dev.maplesadventure.progression.upgrade;

import java.util.EnumMap;
import java.util.Map;

/** Small registry boundary through which future NPC/script adapters expose access. */
public final class UpgradeAccessRegistry {
    private static final Map<UpgradeAccessType, UpgradeAccessValidator> VALIDATORS =
            new EnumMap<>(UpgradeAccessType.class);

    public static synchronized void register(UpgradeAccessType type, UpgradeAccessValidator validator) {
        if (VALIDATORS.putIfAbsent(type, validator) != null)
            throw new IllegalStateException("Duplicate upgrade access validator: " + type);
    }

    static UpgradeAccessValidator validator(UpgradeAccessType type) { return VALIDATORS.get(type); }

    private UpgradeAccessRegistry() {}
}
