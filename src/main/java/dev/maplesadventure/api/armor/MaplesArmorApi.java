package dev.maplesadventure.api.armor;

import dev.maplesadventure.integration.api.ArmorApiBridge;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Read-only Maples armor contribution API; server thread is authoritative. */
public final class MaplesArmorApi {
    private MaplesArmorApi() {}
    /** Empty if the stack has no explicit armor_profiles definition or context is invalid. */
    public static Optional<ArmorProfileView> query(ItemStack stack) { return ArmorApiBridge.query(stack); }
    /** Empty outside the logical server thread; only HEAD/CHEST/LEGS/FEET count. */
    public static Optional<ArmorEquipmentView> equipped(ServerPlayer player) { return ArmorApiBridge.equipped(player); }
}
