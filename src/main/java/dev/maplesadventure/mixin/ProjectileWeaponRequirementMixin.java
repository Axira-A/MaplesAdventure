package dev.maplesadventure.mixin;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
/** Captures bow/crossbow (including firework) source before the weapon is damaged/consumed. */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponRequirementMixin {
    @Redirect(method="shoot",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean maplesadventure$launch(ServerLevel level,Entity projectile,ServerLevel original,LivingEntity shooter,
            InteractionHand hand,ItemStack weapon,List<ItemStack> ammo,float velocity,float inaccuracy,boolean critical,LivingEntity target) {
        dev.maplesadventure.progression.weapon.WeaponDamagePolicy.recordLaunch(projectile,shooter,weapon);
        return level.addFreshEntity(projectile);
    }
}
