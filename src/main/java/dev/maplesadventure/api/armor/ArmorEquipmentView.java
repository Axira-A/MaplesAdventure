package dev.maplesadventure.api.armor;

import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesResistanceType;
import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;

/** Server snapshot of four actual armor slots; totals omit unprofiled equipment. */
public record ArmorEquipmentView(Map<MaplesDamageChannel, Double> totalChannels,
        Map<MaplesResistanceType, Double> totalResistances,
        Map<EquipmentSlot, ArmorProfileView> slots) {
    public ArmorEquipmentView {
        totalChannels = ArmorProfileView.checked(totalChannels, MaplesDamageChannel.class, 4000);
        totalResistances = ArmorProfileView.checked(totalResistances, MaplesResistanceType.class, 4000);
        Objects.requireNonNull(slots);
        slots.forEach((slot, profile) -> {
            if (slot == null || profile == null || !Set.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET).contains(slot)) throw new IllegalArgumentException("Invalid armor slot");
        });
        slots = Map.copyOf(slots);
    }
}
