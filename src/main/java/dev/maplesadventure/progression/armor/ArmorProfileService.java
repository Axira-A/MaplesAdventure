package dev.maplesadventure.progression.armor;

import dev.maplesadventure.MaplesAdventure;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Reload compiled item lookup. Exact rules always take precedence over tag rules. */
public final class ArmorProfileService {
    private static volatile Map<Item, ArmorProfile> compiled = Map.of();
    public static void compile() {
        var next = new IdentityHashMap<Item, ArmorProfile>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            var id = BuiltInRegistries.ITEM.getKey(item);
            var selected = select(ArmorProfileRules.rules(), id,
                    tag -> item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag)));
            selected.ifPresent(rule -> { if (!rule.disabled()) next.put(item, rule.profile()); });
        }
        compiled = Collections.unmodifiableMap(next);
        MaplesAdventure.LOGGER.info("Compiled {} explicit Maples armor profiles", next.size());
    }
    static Optional<ArmorProfileRules.Rule> select(List<ArmorProfileRules.Rule> sorted,
            net.minecraft.resources.ResourceLocation item,
            java.util.function.Predicate<net.minecraft.resources.ResourceLocation> hasTag) {
        var exact = sorted.stream().filter(rule -> item.equals(rule.item())).findFirst();
        return exact.isPresent() ? exact : sorted.stream().filter(rule -> rule.tag() != null && hasTag.test(rule.tag())).findFirst();
    }
    public static Optional<ArmorProfile> find(ItemStack stack) {
        return stack == null || stack.isEmpty() ? Optional.empty() : Optional.ofNullable(compiled.get(stack.getItem()));
    }
    public static void clear() { compiled = Map.of(); }
    private ArmorProfileService() {}
}
