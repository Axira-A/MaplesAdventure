package dev.maplesadventure.progression.defense;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.*;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.status.StatusDamageSources;

/** Explicit adapters, then exact Vanilla sources. Unknown magic/environment damage remains NONE. */
public final class TypedDamageProviderRegistry {
    private record Provider(ResourceLocation id, int priority, TypedDamageChannelProvider delegate) {}
    private static final Map<ResourceLocation, Provider> PROVIDERS = new HashMap<>();
    private static volatile List<Provider> ordered = List.of();
    private static final Set<ResourceLocation> warned = new HashSet<>();
    public static synchronized void register(ResourceLocation id, int priority, TypedDamageChannelProvider provider) {
        if (!PROVIDERS.containsKey(id) && PROVIDERS.size() >= 64) throw new IllegalArgumentException("Typed provider count");
        PROVIDERS.put(Objects.requireNonNull(id), new Provider(id, priority, Objects.requireNonNull(provider)));
        ordered = PROVIDERS.values().stream().sorted(Comparator.comparingInt(Provider::priority).reversed().thenComparing(Provider::id)).toList();
    }
    public static Optional<TypedIncomingDamageContext> resolve(LivingEntity target, DamageSource source, double genericAttackPower,
                                                                Optional<WeaponHitContext> weapon) {
        if (target.level().isClientSide() || !Double.isFinite(genericAttackPower) || genericAttackPower <= 0
                || genericAttackPower > 1_000_000 || StatusDamageSources.isStatus(source)) return Optional.empty();
        if (weapon.isPresent()) return Optional.of(TypedIncomingDamageContext.weapon(weapon.get()));
        for (var provider : ordered) try {
            var result = provider.delegate().channelAttackRatings(source);
            if (result.isPresent()) return Optional.of(TypedIncomingDamageContext.generic(result.get(), "INTEGRATION", provider.id().toString()));
        } catch (RuntimeException | LinkageError failure) {
            if (warned.add(provider.id())) dev.maplesadventure.MaplesAdventure.LOGGER.warn("Invalid typed provider {} ignored", provider.id(), failure);
        }
        // Exact keys AND reliable direct entity. No broad MAGIC/IS_FIRE tag classification.
        var direct = source.getDirectEntity();
        if ((source.is(DamageTypes.ARROW) && direct instanceof AbstractArrow)
                || (source.is(DamageTypes.TRIDENT) && direct instanceof ThrownTrident))
            return single(WeaponDamageChannel.PIERCE, genericAttackPower, "VANILLA_PROJECTILE");
        if (source.is(DamageTypes.FIREBALL) && direct instanceof Fireball && source.getEntity() instanceof LivingEntity)
            return single(WeaponDamageChannel.FIRE, genericAttackPower, "VANILLA_COMBAT_FIRE");
        if (source.is(DamageTypes.FREEZE)) return single(WeaponDamageChannel.ICE, genericAttackPower, "VANILLA_FREEZE");
        if ((source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))
                && direct instanceof LivingEntity mob && direct == source.getEntity()) {
            var resolved = WeaponCombatProfileResolver.resolve(mob.getMainHandItem());
            if (resolved.weapon()) {
                var components = resolved.damage().components();
                double sum = components.stream().mapToDouble(WeaponDamageComponent::baseRatio).sum();
                var powers = new EnumMap<WeaponDamageChannel,Double>(WeaponDamageChannel.class);
                for (var c : components) powers.put(c.channel(), genericAttackPower*c.baseRatio()/sum);
                return Optional.of(TypedIncomingDamageContext.generic(powers, "MOB_WEAPON_TYPES", "base ratios only; no player scaling/requirements"));
            }
            return single(WeaponDamageChannel.PHYSICAL, genericAttackPower, "VANILLA_UNARMED_MELEE");
        }
        return Optional.empty(); // SPELL_TYPED_DEFENSE_NOT_YET_CONNECTED; environment/unknown unchanged.
    }
    private static Optional<TypedIncomingDamageContext> single(WeaponDamageChannel type, double power, String kind) {
        return Optional.of(TypedIncomingDamageContext.generic(Map.of(type,power), kind, "exact Vanilla damage type"));
    }
    private TypedDamageProviderRegistry() {}
}
