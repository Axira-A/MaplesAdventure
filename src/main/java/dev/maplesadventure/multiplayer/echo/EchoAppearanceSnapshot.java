package dev.maplesadventure.multiplayer.echo;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;

/** One bounded equipment/profile snapshot per playback, never per trajectory frame. */
public record EchoAppearanceSnapshot(
        UUID playerId, String profileName,
        ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
        ItemStack mainHand, ItemStack offHand
) {
    public static EchoAppearanceSnapshot capture(ServerPlayer player) {
        return new EchoAppearanceSnapshot(player.getUUID(), player.getGameProfile().getName(),
                copy(player.getItemBySlot(EquipmentSlot.HEAD)), copy(player.getItemBySlot(EquipmentSlot.CHEST)),
                copy(player.getItemBySlot(EquipmentSlot.LEGS)), copy(player.getItemBySlot(EquipmentSlot.FEET)),
                copy(player.getMainHandItem()), copy(player.getOffhandItem()));
    }

    /** Keeps the item identity and bounded visual armor components; arbitrary custom NBT never enters an echo packet. */
    private static ItemStack copy(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack visual = new ItemStack(stack.getItem());
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (dyed != null) visual.set(DataComponents.DYED_COLOR, dyed);
        if (trim != null) visual.set(DataComponents.TRIM, trim);
        return visual;
    }
}
