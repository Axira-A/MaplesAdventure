package dev.maplesadventure.progression.weapon;

import net.minecraft.world.item.ItemStack;

/** The only base-profile -> per-stack transform entry used by combat and UI snapshots. */
public final class WeaponCombatProfileResolver {
    public record Resolved(WeaponRequirementProfile requirements,WeaponScalingProfile scaling,WeaponDamageProfile damage,
                           boolean weapon,WeaponInfusionView infusion,dev.maplesadventure.progression.status.WeaponStatusProfile statuses) {
        public Resolved(WeaponRequirementProfile r,WeaponScalingProfile s,WeaponDamageProfile d,boolean w,WeaponInfusionView i) {
            this(r,s,d,w,i,dev.maplesadventure.progression.status.WeaponStatusProfile.EMPTY);
        }
    }
    public static Resolved resolve(ItemStack stack) {
        var base=resolveBase(stack); var state=WeaponInfusionService.state(stack).orElse(null);
        return resolveInfusion(base,state,WeaponInfusionRegistry.definitions(),WeaponInfusionEligibilityService.eligibility(stack));
    }
    public static Resolved resolveBase(ItemStack stack) {
        var damage=WeaponDamageProfileService.profile(stack);
        return new Resolved(WeaponRequirementService.profile(stack),WeaponScalingService.profile(stack),damage,
                WeaponScalingService.isWeapon(stack)||WeaponDamageProfileService.explicitWeapon(stack),WeaponInfusionView.normal(),
                dev.maplesadventure.progression.status.WeaponStatusRules.profile(stack));
    }
    public static Resolved apply(Resolved base,WeaponInfusionState state,WeaponInfusionDefinition definition,WeaponInfusionEligibility eligibility) {
        if(!eligibility.allows(state.infusionId()))
            return withView(base,WeaponInfusionView.of(definition,WeaponInfusionView.Status.INELIGIBLE));
        if(definition.id().equals(WeaponInfusionRegistry.NORMAL_ID)) return withView(base,WeaponInfusionView.of(definition,WeaponInfusionView.Status.NORMAL));
        var applied=definition.apply(base.scaling(),base.damage());
        return new Resolved(base.requirements(),applied.scaling(),applied.damage(),base.weapon(),WeaponInfusionView.of(definition,WeaponInfusionView.Status.APPLIED),base.statuses().merge(definition.statuses()));
    }
    public static Resolved resolveInfusion(Resolved base,WeaponInfusionState state,
            java.util.Map<net.minecraft.resources.ResourceLocation,WeaponInfusionDefinition> definitions,WeaponInfusionEligibility eligibility) {
        if(state==null) {
            var normal=definitions.get(WeaponInfusionRegistry.NORMAL_ID);
            return normal==null?base:withView(base,WeaponInfusionView.of(normal,WeaponInfusionView.Status.NORMAL));
        }
        var definition=definitions.get(state.infusionId());
        if(definition==null||state.dataVersion()!=WeaponInfusionState.CURRENT_VERSION)
            return withView(base,new WeaponInfusionView(state.infusionId(),"",null,WeaponInfusionBuildup.NONE,WeaponInfusionView.Status.UNKNOWN));
        return apply(base,state,definition,eligibility);
    }
    private static Resolved withView(Resolved base,WeaponInfusionView view) {
        return new Resolved(base.requirements(),base.scaling(),base.damage(),base.weapon(),view,base.statuses());
    }
    private WeaponCombatProfileResolver() {}
}
