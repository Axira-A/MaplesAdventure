package dev.maplesadventure.integration.api;

import dev.maplesadventure.api.weapon.*;
import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesStatusType;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.status.*;
import dev.maplesadventure.config.WeaponRequirementConfig;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Internal-only conversion boundary. No second weapon formula or resolver. */
public final class WeaponApiBridge {
    private static boolean invalid(ItemStack stack) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return stack == null || stack.isEmpty() || server == null || !server.isSameThread();
    }
    public static Optional<WeaponProfileView> query(ItemStack stack) {
        if (invalid(stack)) return Optional.empty();
        var resolved = WeaponCombatProfileResolver.resolve(stack);
        if (!resolved.weapon()) return Optional.empty();
        return Optional.of(profile(stack, resolved));
    }
    public static Optional<WeaponEvaluationView> evaluate(ServerPlayer player, ItemStack stack) {
        if (invalid(stack) || player == null || player.getServer() == null || !player.getServer().isSameThread()
                || player.isRemoved()) return Optional.empty();
        var resolved = WeaponCombatProfileResolver.resolve(stack);
        if (!resolved.weapon()) return Optional.empty();
        var state = PlayerAttributeService.state(player);
        var requirement = WeaponRequirementService.evaluate(state, resolved.requirements(), WeaponRequirementConfig.UNMET_MULTIPLIER.get());
        var bundle = WeaponAttackRatingCalculator.calculate(WeaponClassifier.baseAttack(stack), resolved.scaling(),
                resolved.damage(), state, requirement.damageMultiplier());
        var channels = new EnumMap<MaplesDamageChannel, WeaponAttackChannelView>(MaplesDamageChannel.class);
        bundle.channels().forEach((key, value) -> {
            var channel = channel(key);
            channels.put(channel, new WeaponAttackChannelView(channel, value.base(), value.scalingBonus(), value.attackRating()));
        });
        var status = new EnumMap<MaplesStatusType, Double>(MaplesStatusType.class);
        resolved.statuses().evaluate(state.get(Attribute.ARCANE), resolved.scaling().arcane(), 1).amounts()
                .forEach((key, value) -> status.put(status(key), value));
        var missing = new EnumMap<MaplesWeaponAttribute, Integer>(MaplesWeaponAttribute.class);
        requirement.missingAttributes().forEach((key, value) -> missing.put(attribute(key), value));
        return Optional.of(new WeaponEvaluationView(profile(stack, resolved), requirement.satisfied(), missing,
                requirement.damageMultiplier(), requirement.weaponSkillAllowed(), bundle.totalAttackRating(), channels, status));
    }
    private static WeaponProfileView profile(ItemStack stack, WeaponCombatProfileResolver.Resolved resolved) {
        var requirements = new EnumMap<MaplesWeaponAttribute, Integer>(MaplesWeaponAttribute.class);
        var scaling = new EnumMap<MaplesWeaponAttribute, Double>(MaplesWeaponAttribute.class);
        for (var attr : WeaponRequirementProfile.ATTRIBUTES) {
            requirements.put(attribute(attr), resolved.requirements().get(attr));
            scaling.put(attribute(attr), resolved.scaling().get(attr));
        }
        var damage = resolved.damage().components().stream().map(component ->
                new WeaponDamageComponentView(channel(component.channel()), component.baseRatio(),
                        scaling(component.scaling(resolved.scaling())))).toList();
        var statuses = resolved.statuses().components().stream().map(component ->
                new WeaponStatusComponentView(status(component.type()),
                        component.baseBuildup(), component.arcaneScaling(),
                        statusPolicy(component.policy()))).toList();
        var infusion = resolved.infusion();
        return new WeaponProfileView(BuiltInRegistries.ITEM.getKey(stack.getItem()), WeaponClassifier.baseAttack(stack),
                new WeaponRequirementView(requirements), new WeaponScalingView(scaling, resolved.scaling().maxBonus()),
                damage, statuses, new WeaponInfusionInfo(infusion.id(), infusionState(infusion.status()),
                Optional.ofNullable(infusion.icon())), WeaponInfusionEligibilityService.eligibility(stack).allowed(),
                Optional.ofNullable(resolved.statuses().weightClass()).map(WeaponApiBridge::weightClass));
    }
    private static WeaponScalingView scaling(WeaponScalingProfile profile) {
        var coefficients = new EnumMap<MaplesWeaponAttribute, Double>(MaplesWeaponAttribute.class);
        for (var attr : WeaponRequirementProfile.ATTRIBUTES) coefficients.put(attribute(attr), profile.get(attr));
        return new WeaponScalingView(coefficients, profile.maxBonus());
    }
    private static MaplesWeaponAttribute attribute(Attribute attribute) {
        return switch (attribute) {
            case STRENGTH -> MaplesWeaponAttribute.STRENGTH;
            case DEXTERITY -> MaplesWeaponAttribute.DEXTERITY;
            case INTELLIGENCE -> MaplesWeaponAttribute.INTELLIGENCE;
            case FAITH -> MaplesWeaponAttribute.FAITH;
            case ARCANE -> MaplesWeaponAttribute.ARCANE;
            default -> throw new IllegalArgumentException("Not a weapon attribute");
        };
    }
    private static MaplesDamageChannel channel(WeaponDamageChannel channel) {
        return switch (channel) {
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
    private static MaplesStatusType status(StatusEffectType type) {
        return switch (type) {
            case BLEED -> MaplesStatusType.BLEED;
            case POISON -> MaplesStatusType.POISON;
            case SCARLET_ROT -> MaplesStatusType.SCARLET_ROT;
            case FROSTBITE -> MaplesStatusType.FROSTBITE;
            case SLEEP -> MaplesStatusType.SLEEP;
            case MADNESS -> MaplesStatusType.MADNESS;
            case DEATH_BLIGHT -> MaplesStatusType.DEATH_BLIGHT;
        };
    }
    private static MaplesWeaponStatusArcanePolicy statusPolicy(StatusArcaneScalingPolicy policy) {
        return switch (policy) {
            case NONE -> MaplesWeaponStatusArcanePolicy.NONE;
            case EXPLICIT -> MaplesWeaponStatusArcanePolicy.EXPLICIT;
            case FOLLOW_WEAPON_ARCANE -> MaplesWeaponStatusArcanePolicy.FOLLOW_WEAPON_ARCANE;
        };
    }
    private static MaplesWeaponWeightClass weightClass(StatusWeaponWeightClass weightClass) {
        return switch (weightClass) {
            case THROWING -> MaplesWeaponWeightClass.THROWING;
            case NORMAL -> MaplesWeaponWeightClass.NORMAL;
            case GREAT -> MaplesWeaponWeightClass.GREAT;
            case COLOSSAL -> MaplesWeaponWeightClass.COLOSSAL;
        };
    }
    private static WeaponInfusionInfo.State infusionState(WeaponInfusionView.Status status) {
        return switch (status) {
            case NORMAL -> WeaponInfusionInfo.State.NORMAL;
            case APPLIED -> WeaponInfusionInfo.State.APPLIED;
            case UNKNOWN -> WeaponInfusionInfo.State.UNKNOWN;
            case INELIGIBLE -> WeaponInfusionInfo.State.INELIGIBLE;
        };
    }
    private WeaponApiBridge() {}
}
