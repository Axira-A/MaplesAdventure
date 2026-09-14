package dev.maplesadventure.progression.client;

import dev.maplesadventure.progression.stats.*;
import java.util.Locale;
import net.minecraft.network.chat.Component;

final class CharacterStatFormatting {
    static String value(CharacterStat stat, CharacterStatValue value) {
        if (!value.available()) return "—";
        return stat.percentage() ? String.format(Locale.ROOT, "%.1f%%", value.value() * 100D)
                : String.format(Locale.ROOT, "%.1f", value.value());
    }

    static Component comparison(CharacterStat stat, CharacterStatValue before, CharacterStatValue after) {
        if (!before.available() && !after.available())
            return Component.translatable(StatImplementationState.UNAVAILABLE.translationKey());
        if (!after.available()) return Component.literal("—");
        if (!after.differsFrom(before) || !before.available()) return Component.literal(value(stat, after));
        return Component.literal(value(stat, before) + "  →  " + value(stat, after));
    }

    private CharacterStatFormatting() {}
}
