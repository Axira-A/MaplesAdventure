package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.progression.Attribute;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Bounded data-driven transform applied only after the base item profile has resolved. */
public record WeaponInfusionDefinition(ResourceLocation id, String translationKey, ResourceLocation icon,
        double baseMultiplier, Map<Attribute, AttributeTransform> physicalScaling,
        ElementSplit elementSplit, WeaponInfusionBuildup futureBuildup, dev.maplesadventure.progression.status.WeaponStatusProfile statuses) {
    public WeaponInfusionDefinition(ResourceLocation id,String key,ResourceLocation icon,double base,Map<Attribute,AttributeTransform> transforms,
            ElementSplit split,WeaponInfusionBuildup legacy) {
        this(id,key,icon,base,transforms,split,legacy,dev.maplesadventure.progression.status.WeaponStatusProfile.legacy(legacy));
    }
    public static final int MAX_DEFINITIONS = 16;

    public record AttributeTransform(double multiplier, double minimum, double maximum) {
        public AttributeTransform {
            for (double value : new double[]{multiplier, minimum, maximum})
                if (!Double.isFinite(value) || value < 0 || value > 2) throw new IllegalArgumentException("Infusion scaling bounds");
            if (minimum > maximum || maximum > 1.5) throw new IllegalArgumentException("Infusion scaling clamp bounds");
        }
        public double apply(double base) { return Math.clamp(base * multiplier, minimum, maximum); }
        public static AttributeTransform identity() { return new AttributeTransform(1, 0, 1.5); }
    }

    public record ElementSplit(WeaponDamageChannel channel, double physicalRatio, double elementRatio,
                               WeaponScalingProfile elementScaling) {
        public ElementSplit {
            if (channel == null || !channel.elemental()) throw new IllegalArgumentException("Split channel must be elemental");
            if (!Double.isFinite(physicalRatio) || !Double.isFinite(elementRatio) || physicalRatio < 0 || elementRatio <= 0
                    || physicalRatio > 2 || elementRatio > 2 || physicalRatio + elementRatio > 2)
                throw new IllegalArgumentException("Infusion split bounds");
            Objects.requireNonNull(elementScaling);
        }
    }

    public record Applied(WeaponScalingProfile scaling, WeaponDamageProfile damage) {}

    public WeaponInfusionDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(translationKey); Objects.requireNonNull(futureBuildup);
        if (id.toString().length() > 256 || translationKey.length() > 128 || icon != null && icon.toString().length() > 256)
            throw new IllegalArgumentException("Infusion metadata bounds");
        if (!Double.isFinite(baseMultiplier) || baseMultiplier <= 0 || baseMultiplier > 2)
            throw new IllegalArgumentException("Infusion base multiplier bounds");
        var copy = new EnumMap<Attribute, AttributeTransform>(Attribute.class); copy.putAll(physicalScaling);
        for (Attribute attribute : WeaponRequirementProfile.ATTRIBUTES) copy.putIfAbsent(attribute, AttributeTransform.identity());
        physicalScaling = Collections.unmodifiableMap(copy);
    }

    public Applied apply(WeaponScalingProfile baseScaling, WeaponDamageProfile baseDamage) {
        if (id.equals(WeaponInfusionRegistry.NORMAL_ID)) return new Applied(baseScaling, baseDamage);
        if (baseDamage.components().stream().noneMatch(component -> component.channel().physical()))
            throw new IllegalArgumentException("Infusion requires a physical component");
        var infusedWeaponScaling = transform(baseScaling);
        var components = new ArrayList<WeaponDamageComponent>();
        double physicalRatio = 0;
        for (var component : baseDamage.components()) {
            var sourceScaling = component.scaling(baseScaling);
            if (component.channel().physical()) {
                physicalRatio += component.baseRatio();
                double ratio = component.baseRatio() * baseMultiplier * (elementSplit == null ? 1 : elementSplit.physicalRatio());
                components.add(override(component.channel(), ratio, transform(sourceScaling)));
            } else {
                // An explicitly eligible special weapon keeps its existing elemental channels and their original scaling.
                components.add(override(component.channel(), component.baseRatio(), sourceScaling));
            }
        }
        if (elementSplit != null) {
            if (baseDamage.components().stream().anyMatch(component -> component.channel() == elementSplit.channel()))
                throw new IllegalArgumentException("Infusion element duplicates an existing damage channel");
            components.add(override(elementSplit.channel(), physicalRatio * baseMultiplier * elementSplit.elementRatio(), elementSplit.elementScaling()));
        }
        return new Applied(infusedWeaponScaling, new WeaponDamageProfile(components, "INFUSION", baseDamage.weaponClass(), id.toString()));
    }

    private WeaponScalingProfile transform(WeaponScalingProfile source) {
        return new WeaponScalingProfile(rule(Attribute.STRENGTH).apply(source.strength()),
                rule(Attribute.DEXTERITY).apply(source.dexterity()), rule(Attribute.INTELLIGENCE).apply(source.intelligence()),
                rule(Attribute.FAITH).apply(source.faith()), rule(Attribute.ARCANE).apply(source.arcane()), source.maxBonus(),
                "INFUSION", source.weaponClass(), id.toString());
    }
    private AttributeTransform rule(Attribute attribute) { return physicalScaling.get(attribute); }
    private static WeaponDamageComponent override(WeaponDamageChannel channel, double ratio, WeaponScalingProfile scaling) {
        return new WeaponDamageComponent(channel, ratio, WeaponDamageComponent.ScalingMode.OVERRIDE, scaling);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(id.toString(), 256); buffer.writeUtf(translationKey, 128);
        buffer.writeBoolean(icon != null); if (icon != null) buffer.writeUtf(icon.toString(), 256);
        buffer.writeDouble(baseMultiplier);
        for (Attribute attribute : WeaponRequirementProfile.ATTRIBUTES) {
            var value = rule(attribute); buffer.writeDouble(value.multiplier()); buffer.writeDouble(value.minimum()); buffer.writeDouble(value.maximum());
        }
        buffer.writeBoolean(elementSplit != null);
        if (elementSplit != null) {
            buffer.writeVarInt(elementSplit.channel().ordinal()); buffer.writeDouble(elementSplit.physicalRatio());
            buffer.writeDouble(elementSplit.elementRatio()); WeaponRequirementNetwork.writeScaling(buffer, elementSplit.elementScaling());
        }
        buffer.writeVarInt(futureBuildup.ordinal()); statuses.write(buffer);
    }

    public static WeaponInfusionDefinition read(RegistryFriendlyByteBuf buffer) {
        var id = ResourceLocation.parse(buffer.readUtf(256)); var key = buffer.readUtf(128);
        ResourceLocation icon = buffer.readBoolean() ? ResourceLocation.parse(buffer.readUtf(256)) : null;
        double base = buffer.readDouble(); var rules = new EnumMap<Attribute, AttributeTransform>(Attribute.class);
        for (Attribute attribute : WeaponRequirementProfile.ATTRIBUTES)
            rules.put(attribute, new AttributeTransform(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        ElementSplit split = null;
        if (buffer.readBoolean()) {
            int channel = buffer.readVarInt();
            if (channel < 0 || channel >= WeaponDamageChannel.values().length) throw new IllegalArgumentException("Infusion channel bounds");
            split = new ElementSplit(WeaponDamageChannel.values()[channel], buffer.readDouble(), buffer.readDouble(), WeaponRequirementNetwork.readScaling(buffer));
        }
        int buildup = buffer.readVarInt();
        if (buildup < 0 || buildup >= WeaponInfusionBuildup.values().length) throw new IllegalArgumentException("Infusion buildup bounds");
        return new WeaponInfusionDefinition(id, key, icon, base, rules, split, WeaponInfusionBuildup.values()[buildup],dev.maplesadventure.progression.status.WeaponStatusProfile.read(buffer));
    }
}
