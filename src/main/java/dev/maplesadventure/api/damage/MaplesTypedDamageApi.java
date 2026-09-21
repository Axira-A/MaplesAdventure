package dev.maplesadventure.api.damage;

import net.minecraft.resources.ResourceLocation;
import dev.maplesadventure.api.integration.TypedDamageProvider;
import dev.maplesadventure.integration.api.ApiNotifications;
import dev.maplesadventure.progression.defense.TypedDamageProviderRegistry;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Registration facade; never initiates damage. Existing weapon snapshots take precedence. */
public final class MaplesTypedDamageApi {
    private MaplesTypedDamageApi() {}
    /**
     * Registers at common setup (enqueueWork), once per stable ID. Re-registering the ID replaces
     * it. Higher priority wins, equal priority sorts by ID. Providers persist across world reloads.
     * Existing generic typed mitigation currently applies to players; this does not enable a new
     * non-weapon enemy mitigation system. The registry isolates invalid provider results.
     *
     * @param id unique integration ID
     * @param priority ordering priority
     *
     * @param provider pure callback without client/optional-mod hard dependencies
     */
    public static void register(ResourceLocation id, int priority, TypedDamageProvider provider) {
        java.util.Objects.requireNonNull(provider);
        TypedDamageProviderRegistry.register(id, priority, source -> ApiNotifications.readOnly(() ->
                provider.describe(source).map(value -> {
                    var map = new java.util.EnumMap<WeaponDamageChannel, Double>(WeaponDamageChannel.class);
                    value.channels().forEach((k,v) -> map.put(WeaponDamageChannel.valueOf(k.name()), v));
                    return java.util.Map.copyOf(map);
                })));
    }
}
