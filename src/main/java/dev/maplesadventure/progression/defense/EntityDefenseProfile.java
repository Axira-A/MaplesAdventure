package dev.maplesadventure.progression.defense;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

public record EntityDefenseProfile(ResourceLocation profileId, Map<WeaponDamageChannel, ChannelDefense> channels, String source) {
    public static final EntityDefenseProfile NONE = new EntityDefenseProfile(
            ResourceLocation.fromNamespaceAndPath("maplesadventure", "none"), Map.of(), "NONE");
    public EntityDefenseProfile {
        Objects.requireNonNull(profileId); Objects.requireNonNull(source);
        var copy = new EnumMap<WeaponDamageChannel, ChannelDefense>(WeaponDamageChannel.class);
        channels.forEach((channel, defense) -> copy.put(Objects.requireNonNull(channel), Objects.requireNonNull(defense)));
        channels = Collections.unmodifiableMap(copy);
    }
    public ChannelDefense channel(WeaponDamageChannel channel) { return channels.getOrDefault(channel, ChannelDefense.NONE); }
}
