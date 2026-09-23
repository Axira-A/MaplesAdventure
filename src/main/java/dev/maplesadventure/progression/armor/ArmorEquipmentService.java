package dev.maplesadventure.progression.armor;

import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import dev.maplesadventure.progression.status.StatusResistanceType;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/** Only the four actual humanoid armor slots contribute. */
public final class ArmorEquipmentService {
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static ArmorEquipmentSnapshot snapshot(ServerPlayer player) {
        var channels = new EnumMap<WeaponDamageChannel, Double>(WeaponDamageChannel.class);
        var resistances = new EnumMap<StatusResistanceType, Double>(StatusResistanceType.class);
        for (var slot : SLOTS) ArmorProfileService.find(player.getItemBySlot(slot)).ifPresent(profile -> {
            profile.channels().forEach((type, value) -> channels.merge(type, value, Double::sum));
            profile.resistances().forEach((type, value) -> resistances.merge(type, value, Double::sum));
        });
        return new ArmorEquipmentSnapshot(channels, resistances);
    }
    private ArmorEquipmentService() {}
}
