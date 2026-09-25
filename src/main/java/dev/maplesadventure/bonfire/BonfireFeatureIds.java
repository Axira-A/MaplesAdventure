package dev.maplesadventure.bonfire;

import dev.maplesadventure.MaplesAdventure;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Bounded namespaced persistence, independent of which addons happen to be installed. */
public final class BonfireFeatureIds {
    public static final int MAX_FEATURES = 64;
    public static final int MAX_ID_LENGTH = 128;
    public static ResourceLocation parse(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > MAX_ID_LENGTH) return null;
        String qualified = raw.contains(":") ? raw : "maplesadventure:" + raw;
        return qualified.length() <= MAX_ID_LENGTH ? ResourceLocation.tryParse(qualified) : null;
    }
    public static Set<ResourceLocation> load(ListTag tags) {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        boolean invalid = tags.size() > MAX_FEATURES;
        for (int i = 0; i < Math.min(tags.size(), MAX_FEATURES); i++) {
            ResourceLocation id = parse(tags.getString(i));
            if (id == null) invalid = true;
            else result.add(id);
        }
        if (invalid) MaplesAdventure.LOGGER.warn("Discarded invalid/oversized bonfire feature configuration");
        return result;
    }
    public static ListTag save(Set<ResourceLocation> ids) {
        if (ids.size() > MAX_FEATURES) throw new IllegalArgumentException("Too many bonfire features");
        ListTag result = new ListTag();
        ids.stream().sorted(java.util.Comparator.comparing(ResourceLocation::toString)).forEach(id -> {
            if (parse(id.toString()) == null) throw new IllegalArgumentException("Invalid feature ID");
            result.add(StringTag.valueOf(id.toString()));
        });
        return result;
    }
    private BonfireFeatureIds() {}
}
