package dev.maplesadventure.progression.weapon;
import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.item.ItemStack;
public final class WeaponDamagePolicy {
    /** Explicit public adapter entry: caller must own a reliable shooter+weapon launch context. */
    public static void recordLaunch(Entity projectile,LivingEntity shooter,ItemStack weapon) {
        var resolved=WeaponCombatProfileResolver.resolve(weapon);
        if(!(shooter instanceof ServerPlayer player) || !(projectile instanceof Projectile)
                || projectile.hasData(ProgressionAttachments.PROJECTILE_REQUIREMENT)
                || (!resolved.requirements().enabled() && !resolved.weapon() && resolved.statuses().components().isEmpty())) return;
        var context=withMotion(directContext(player,weapon),dev.maplesadventure.progression.status.StatusMotionValueResolver.projectile((Projectile)projectile,resolved.requirements().weaponClass()).value());
        projectile.setData(ProgressionAttachments.PROJECTILE_REQUIREMENT,new ProjectileRequirementPenalty(context));
    }
    /** Legacy adapter name; callers should use directContext().bundle().nominalMultiplier(). */
    @Deprecated public static double scalingMultiplier(ServerPlayer player,ItemStack weapon) {
        return directContext(player,weapon).bundle().nominalMultiplier();
    }
    public static double weaponMultiplier(ServerPlayer player,ItemStack weapon) {
        return directContext(player,weapon).effectiveMultiplier();
    }
    public static WeaponHitContext directContext(ServerPlayer player,ItemStack weapon) {
        var resolved=WeaponCombatProfileResolver.resolve(weapon);
        var requirements=WeaponRequirementService.evaluate(dev.maplesadventure.progression.PlayerAttributeService.state(player),resolved.requirements(),
                dev.maplesadventure.config.WeaponRequirementConfig.UNMET_MULTIPLIER.get());
        // Base cancels from the current multiplier; cached facts avoid modifier scans in the damage hot path.
        var facts=WeaponRequirementService.facts(weapon.getItem());
        double base=facts==null?1:facts.damage();
        var bundle=resolved.weapon()?WeaponAttackRatingCalculator.calculate(base,resolved.scaling(),resolved.damage(),
                dev.maplesadventure.progression.PlayerAttributeService.state(player),requirements.damageMultiplier()):WeaponDamageBundle.legacy(1,requirements.damageMultiplier());
        return new WeaponHitContext(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(weapon.getItem()),bundle,requirements,false,player.getUUID(),
                resolved.statuses().evaluate(dev.maplesadventure.progression.PlayerAttributeService.get(player,dev.maplesadventure.progression.Attribute.ARCANE),resolved.scaling().arcane(),1));
    }
    public static double multiplier(DamageSource source) { return context(source).map(WeaponHitContext::effectiveMultiplier).orElse(1.0); }
    private static WeaponHitContext withMotion(WeaponHitContext context,double motion) {
        return new WeaponHitContext(context.usedWeapon(),context.bundle(),context.requirements(),context.projectileSnapshot(),context.owner(),
                dev.maplesadventure.progression.status.StatusMotionValueResolver.apply(context.statuses(),motion));
    }
    private static WeaponHitContext hitContext(ServerPlayer player,ItemStack weapon,DamageSource source) {
        return withMotion(directContext(player,weapon),dev.maplesadventure.progression.status.StatusMotionValueResolver.resolve(source,
                WeaponRequirementService.profile(weapon).weaponClass()).value());
    }
    public static java.util.Optional<WeaponHitContext> context(DamageSource source) {
        // Independent secondary damage stays independent even when it retains a weapon/projectile owner.
        if(dev.maplesadventure.progression.status.StatusDamageSources.isStatus(source) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return java.util.Optional.empty();
        var direct=source.getDirectEntity();
        if(direct instanceof Projectile) {
            var snapshot=direct.getExistingData(ProgressionAttachments.PROJECTILE_REQUIREMENT).orElse(null);
            return snapshot==null?java.util.Optional.empty():java.util.Optional.of(snapshot.context());
        }
        if(!(source.getEntity() instanceof ServerPlayer player) || direct!=player) return java.util.Optional.empty();
        if(WeaponIntegrations.epic!=null) {
            var weapon=WeaponIntegrations.epic.usedWeapon(source);
            if(weapon.isPresent()) return java.util.Optional.of(hitContext(player,weapon.get(),source));
        }
        // Vanilla Player.attack is main-hand only. Never infer weapons for magic/fire/explosion/etc.
        if(source.is(DamageTypes.PLAYER_ATTACK)) return java.util.Optional.of(hitContext(player,player.getMainHandItem(),source));
        return java.util.Optional.empty();
    }
    private WeaponDamagePolicy() {}
}
