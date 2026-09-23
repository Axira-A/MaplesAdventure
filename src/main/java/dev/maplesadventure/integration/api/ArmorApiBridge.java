package dev.maplesadventure.integration.api;

import dev.maplesadventure.api.armor.*;
import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesResistanceType;
import dev.maplesadventure.progression.armor.*;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import dev.maplesadventure.progression.status.StatusResistanceType;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Internal conversion from compiled armor data to detached public views. */
public final class ArmorApiBridge {
    public static Optional<ArmorProfileView> query(ItemStack stack) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (stack == null || stack.isEmpty() || server == null || !server.isSameThread()) return Optional.empty();
        return ArmorProfileService.find(stack).map(profile -> view(stack, profile));
    }
    public static Optional<ArmorEquipmentView> equipped(ServerPlayer player) {
        if (player == null || player.getServer() == null || !player.getServer().isSameThread() || player.isRemoved())
            return Optional.empty();
        var snapshot = ArmorEquipmentService.snapshot(player);
        var channels = new EnumMap<MaplesDamageChannel, Double>(MaplesDamageChannel.class);
        var resistances = new EnumMap<MaplesResistanceType, Double>(MaplesResistanceType.class);
        snapshot.channels().forEach((type, value) -> channels.put(channel(type), value));
        snapshot.resistances().forEach((type, value) -> resistances.put(resistance(type), value));
        var slots = new EnumMap<EquipmentSlot, ArmorProfileView>(EquipmentSlot.class);
        for (var slot : ArmorEquipmentService.SLOTS) {
            var stack = player.getItemBySlot(slot);
            ArmorProfileService.find(stack).ifPresent(profile -> slots.put(slot, view(stack, profile)));
        }
        return Optional.of(new ArmorEquipmentView(channels, resistances, slots));
    }
    private static ArmorProfileView view(ItemStack stack, ArmorProfile profile) {
        var channels = new EnumMap<MaplesDamageChannel, Double>(MaplesDamageChannel.class);
        var resistances = new EnumMap<MaplesResistanceType, Double>(MaplesResistanceType.class);
        profile.channels().forEach((type, value) -> channels.put(channel(type), value));
        profile.resistances().forEach((type, value) -> resistances.put(resistance(type), value));
        return new ArmorProfileView(BuiltInRegistries.ITEM.getKey(stack.getItem()), channels, resistances);
    }
    private static MaplesDamageChannel channel(WeaponDamageChannel type) {
        return switch (type) {
            case PHYSICAL -> MaplesDamageChannel.PHYSICAL;
            case SLASH -> MaplesDamageChannel.SLASH;
            case STRIKE -> MaplesDamageChannel.STRIKE;
            case PIERCE -> MaplesDamageChannel.PIERCE;
            case MAGIC -> MaplesDamageChannel.MAGIC;
            case FIRE -> MaplesDamageChannel.FIRE;
            case LIGHTNING -> MaplesDamageChannel.LIGHTNING;
            case ICE -> MaplesDamageChannel.ICE;
            case HOLY -> MaplesDamageChannel.HOLY;
        };
    }
    private static MaplesResistanceType resistance(StatusResistanceType type) {
        return switch (type) {
            case IMMUNITY -> MaplesResistanceType.IMMUNITY;
            case ROBUSTNESS -> MaplesResistanceType.ROBUSTNESS;
            case FOCUS -> MaplesResistanceType.FOCUS;
            case VITALITY -> MaplesResistanceType.VITALITY;
        };
    }
    private ArmorApiBridge() {}
}
