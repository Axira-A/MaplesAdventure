package dev.maplesadventure.api.weapon;

import dev.maplesadventure.integration.api.WeaponApiBridge;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Read-only weapon API. Query/evaluate require the logical server thread after registry compilation. */
public final class MaplesWeaponApi {
    private MaplesWeaponApi() {}
    /** Empty for invalid context, empty stack or an ordinary item without a resolved Maples weapon profile. */
    public static Optional<WeaponProfileView> query(ItemStack stack) { return WeaponApiBridge.query(stack); }
    /** Empty for invalid context; all numerical results use current server attributes and configuration. */
    public static Optional<WeaponEvaluationView> evaluate(ServerPlayer player, ItemStack stack) {
        return WeaponApiBridge.evaluate(player, stack);
    }
}
