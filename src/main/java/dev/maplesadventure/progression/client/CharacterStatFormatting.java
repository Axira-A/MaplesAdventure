package dev.maplesadventure.progression.client;

import dev.maplesadventure.progression.stats.*;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;

final class CharacterStatFormatting {
    static Optional<Component> implementationHint(StatImplementationState state) {
        return state == StatImplementationState.ACTIVE ? Optional.empty()
                : Optional.of(Component.translatable(state.translationKey()));
    }

    static Optional<Component> handDescription(CharacterStatsSnapshot snapshot, CharacterStat stat) {
        return snapshot.handState(stat).flatMap(hand -> switch (hand) {
            case EMPTY -> Optional.of(Component.translatable("screen.maplesadventure.weapon.empty_hand"));
            case NON_WEAPON -> Optional.of(Component.translatable("screen.maplesadventure.weapon.no_weapon"));
            case WEAPON -> Optional.empty();
        });
    }

    static Component comparison(CharacterStat stat, CharacterStatsSnapshot before, CharacterStatsSnapshot after) {
        return handDescription(after, stat).orElseGet(() -> comparison(stat, before.value(stat), after.value(stat)));
    }

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
