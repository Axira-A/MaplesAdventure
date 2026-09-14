package dev.maplesadventure.progression.spell;

import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.stats.StatImplementationState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record SpellSchoolStat(SpellSchoolScalingProfile profile, Component displayName,
        double progressionBonus, SpellPowerContext context, boolean modifierPresent) {
    public SpellSchoolStat {
        if (profile == null || displayName == null || context == null || !Double.isFinite(progressionBonus)
                || progressionBonus < 0 || progressionBonus > 10) throw new IllegalArgumentException("Invalid school stat");
        displayName = displayName.copy();
    }
    public ResourceLocation schoolId() { return profile.schoolId(); }
    public StatImplementationState implementationState() {
        return context.available() ? StatImplementationState.ACTIVE : StatImplementationState.PREVIEW_ONLY;
    }
    public double runtimeSchoolPower() { return context.schoolPower(progressionBonus); }
    public double runtimeSpellPower() { return context.effectivePower(progressionBonus); }
    public SpellSchoolStat preview(PlayerAttributeState state) {
        return new SpellSchoolStat(profile, displayName, profile.bonus(state), context, modifierPresent);
    }
}
