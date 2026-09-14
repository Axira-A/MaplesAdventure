package dev.maplesadventure.integration.irons.progression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.mixin.IronsSchoolPowerAccessor;
import dev.maplesadventure.progression.PlayerAttributeService;
import dev.maplesadventure.progression.spell.*;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;

/** One ADD_VALUE contribution per school; never mutates base values or final spell damage. */
public final class IronsSpellScalingAdapter implements SpellScalingAdapter {
    private static final String PREFIX = "progression_spell_power/";
    private final Set<String> warned = new HashSet<>();
    public static ResourceLocation modifierId(ResourceLocation school) {
        return ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, PREFIX + school.getNamespace() + "/" + school.getPath());
    }
    @Override public void refresh(ServerPlayer player) {
        // Also remove persisted modifiers from profiles removed by /reload. Never remove another mod's modifiers.
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            if (!player.getAttributes().hasAttribute(holder)) return;
            var instance = player.getAttribute(holder);
            for (var modifier : List.copyOf(instance.getModifiers()))
                if (modifier.id().getNamespace().equals(MaplesAdventure.MOD_ID) && modifier.id().getPath().startsWith(PREFIX))
                    instance.removeModifier(modifier.id());
        });
        var state = PlayerAttributeService.state(player);
        Set<Holder<Attribute>> claimed = new HashSet<>();
        for (var profile : SpellSchoolScalingRegistry.profiles().values()) {
            var school = SchoolRegistry.REGISTRY.get(profile.schoolId());
            if (school == null) { warn("Unregistered school " + profile.schoolId()); continue; }
            var holder = ((IronsSchoolPowerAccessor) school).maplesadventure$powerAttribute();
            if (!claimed.add(holder)) { warn("Schools share a power attribute; refused duplicate modifier for " + profile.schoolId()); continue; }
            var instance = player.getAttribute(holder);
            if (instance == null) { warn("Player has no power attribute for " + profile.schoolId()); continue; }
            double bonus = profile.bonus(state);
            if (bonus != 0) instance.addOrReplacePermanentModifier(new AttributeModifier(modifierId(profile.schoolId()),
                    bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }
    private void warn(String message) {
        if (warned.add(message)) MaplesAdventure.LOGGER.warn("[SpellScaling] {}", message);
    }
    @Override public SpellSchoolScalingSnapshot snapshot(ServerPlayer player) {
        var state = PlayerAttributeService.state(player);
        Map<ResourceLocation, SpellSchoolStat> result = new LinkedHashMap<>();
        for (var profile : SpellSchoolScalingRegistry.profiles().values()) {
            var school = SchoolRegistry.REGISTRY.get(profile.schoolId());
            var context = SpellPowerContext.unavailable();
            boolean present = false;
            Component name = Component.translatable("school." + profile.schoolId().getNamespace() + "." + profile.schoolId().getPath());
            if (school != null) {
                name = school.getDisplayName();
                var instance = player.getAttribute(((IronsSchoolPowerAccessor) school).maplesadventure$powerAttribute());
                if (instance != null && instance.getAttribute().value() instanceof RangedAttribute range) {
                    var ownId = modifierId(profile.schoolId());
                    present = instance.getModifier(ownId) != null;
                    double additive = instance.getBaseValue(), multipliedBase = 1, total = 1;
                    for (var modifier : instance.getModifiers()) {
                        if (modifier.id().equals(ownId)) continue;
                        switch (modifier.operation()) {
                            case ADD_VALUE -> additive += modifier.amount();
                            case ADD_MULTIPLIED_BASE -> multipliedBase += modifier.amount();
                            case ADD_MULTIPLIED_TOTAL -> total *= 1 + modifier.amount();
                        }
                    }
                    var model = new SpellPowerContext(additive, multipliedBase * total, range.getMinValue(), range.getMaxValue(),
                            player.getAttributeValue(AttributeRegistry.SPELL_POWER), school.getPowerFor(player), true);
                    // An addon may override getPowerFor. Do not claim a linear preview for an unknown custom formula.
                    double actualOwn = present ? instance.getModifier(ownId).amount() : 0;
                    boolean matches = Math.abs(model.schoolPower(actualOwn) - school.getPowerFor(player)) < 1e-6
                            && Math.abs(actualOwn - profile.bonus(state)) < 1e-6;
                    context = new SpellPowerContext(additive, multipliedBase * total, range.getMinValue(), range.getMaxValue(),
                            model.globalPower(), model.observedSchoolPower(), matches);
                }
            }
            result.put(profile.schoolId(), new SpellSchoolStat(profile, name, profile.bonus(state), context, present));
        }
        return new SpellSchoolScalingSnapshot(result);
    }
}
