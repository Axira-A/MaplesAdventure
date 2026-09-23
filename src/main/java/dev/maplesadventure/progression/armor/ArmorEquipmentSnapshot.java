package dev.maplesadventure.progression.armor;

import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import dev.maplesadventure.progression.status.StatusResistanceType;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Thirteen detached server-authored equipment contributions; no ItemStack NBT on the wire. */
public record ArmorEquipmentSnapshot(Map<WeaponDamageChannel, Double> channels,
                                     Map<StatusResistanceType, Double> resistances) {
    public static final ArmorEquipmentSnapshot EMPTY = new ArmorEquipmentSnapshot(Map.of(), Map.of());
    public ArmorEquipmentSnapshot {
        channels = checked(channels, WeaponDamageChannel.class);
        resistances = checked(resistances, StatusResistanceType.class);
    }
    private static <E extends Enum<E>> Map<E, Double> checked(Map<E, Double> input, Class<E> type) {
        Objects.requireNonNull(input);
        var result = new EnumMap<E, Double>(type);
        input.forEach((key, value) -> {
            Objects.requireNonNull(key);
            if (value == null || !Double.isFinite(value) || value < 0 || value > 4000)
                throw new IllegalArgumentException("Armor equipment total outside 0..4000");
            result.put(key, value);
        });
        return Collections.unmodifiableMap(result);
    }
    public double channel(WeaponDamageChannel type) { return channels.getOrDefault(type, 0.0); }
    public double resistance(StatusResistanceType type) { return resistances.getOrDefault(type, 0.0); }
    public void write(RegistryFriendlyByteBuf buffer) {
        for (var type : WeaponDamageChannel.values()) buffer.writeDouble(channel(type));
        for (var type : StatusResistanceType.values()) buffer.writeDouble(resistance(type));
    }
    public static ArmorEquipmentSnapshot read(RegistryFriendlyByteBuf buffer) {
        var channels = new EnumMap<WeaponDamageChannel, Double>(WeaponDamageChannel.class);
        var resistances = new EnumMap<StatusResistanceType, Double>(StatusResistanceType.class);
        for (var type : WeaponDamageChannel.values()) channels.put(type, buffer.readDouble());
        for (var type : StatusResistanceType.values()) resistances.put(type, buffer.readDouble());
        return new ArmorEquipmentSnapshot(channels, resistances);
    }
}
