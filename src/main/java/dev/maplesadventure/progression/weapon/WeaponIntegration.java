package dev.maplesadventure.progression.weapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import java.util.Optional;
public interface WeaponIntegration {
    String category(ItemStack stack);
    /** Empty Optional means not this integration; Optional of EMPTY means an explicit unarmed hit. */
    Optional<ItemStack> usedWeapon(DamageSource source);
}
