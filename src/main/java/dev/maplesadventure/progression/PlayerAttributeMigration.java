package dev.maplesadventure.progression;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Versioned, fail-safe normalization of persistent player progression data. */
public final class PlayerAttributeMigration {
    public static final int CURRENT_VERSION = 1;
    public static final int MINIMUM_VALUE = ProgressionCurve.BASE_ATTRIBUTE;
    public static final int ABSOLUTE_HARD_CAP = ProgressionCurve.ABSOLUTE_HARD_CAP;

    public static Result migrate(CompoundTag tag) {
        int sourceVersion = tag.contains("DataVersion", Tag.TAG_ANY_NUMERIC) ? tag.getInt("DataVersion") : 0;
        boolean corrected = sourceVersion != CURRENT_VERSION;
        EnumMap<Attribute, Integer> values = new EnumMap<>(Attribute.class);
        for (Attribute attribute : Attribute.values()) {
            String key = nbtKey(attribute);
            int raw = tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : MINIMUM_VALUE;
            if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) corrected = true;
            int normalized = Math.clamp(raw, MINIMUM_VALUE, ABSOLUTE_HARD_CAP);
            if (normalized != raw) corrected = true;
            values.put(attribute, normalized);
        }
        return new Result(CURRENT_VERSION, values, corrected, sourceVersion);
    }

    static String nbtKey(Attribute attribute) {
        String name = attribute.serializedName();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public record Result(int dataVersion, Map<Attribute, Integer> values, boolean corrected, int sourceVersion) {
        public Result { values = Map.copyOf(values); }
    }

    private PlayerAttributeMigration() {}
}
