package dev.maplesadventure.progression.weapon;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
public final class WeaponRequirementCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new WeaponRequirementCommands()); }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        for(String root:new String[]{"ma","maplesadventure"}) event.getDispatcher().register(Commands.literal(root)
            .then(Commands.literal("weapon").requires(s->s.hasPermission(2))
                .then(Commands.literal("status").executes(c->status(c.getSource())))
                .then(Commands.literal("damage").executes(c->damage(c.getSource(),c.getSource().getPlayerOrException().getMainHandItem()))
                    .then(Commands.argument("item",ItemArgument.item(event.getBuildContext())).executes(c->damage(c.getSource(),ItemArgument.getItem(c,"item").createItemStack(1,false)))))
                .then(Commands.literal("scaling").executes(c->scaling(c.getSource(),c.getSource().getPlayerOrException().getMainHandItem()))
                    .then(Commands.argument("item",ItemArgument.item(event.getBuildContext())).executes(c->scaling(c.getSource(),ItemArgument.getItem(c,"item").createItemStack(1,false)))))
                .then(Commands.literal("requirement").executes(c->info(c.getSource(),c.getSource().getPlayerOrException().getMainHandItem()))
                    .then(Commands.argument("item",ItemArgument.item(event.getBuildContext())).executes(c->info(c.getSource(),ItemArgument.getItem(c,"item").createItemStack(1,false)))))
                .then(Commands.literal("infusion")
                    .then(Commands.literal("info").executes(c->infusionInfo(c.getSource())))
                    .then(Commands.literal("set").then(Commands.argument("id",ResourceLocationArgument.id())
                        .suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggestResource(WeaponInfusionRegistry.definitions().keySet(),b))
                        .executes(c->setInfusion(c.getSource(),ResourceLocationArgument.getId(c,"id")))))
                    .then(Commands.literal("clear").executes(c->clearInfusion(c.getSource()))))
                .then(Commands.literal("audit").executes(c->{
                    WeaponRequirementService.compiled().entrySet().stream().sorted(java.util.Comparator.comparing(e->BuiltInRegistries.ITEM.getKey(e.getKey())))
                        .forEach(e->info(c.getSource(),e.getKey().getDefaultInstance())); return 1;
                }))));
    }
    private static int info(CommandSourceStack source,ItemStack stack) {
        var p=WeaponCombatProfileResolver.resolve(stack).requirements(); var f=WeaponRequirementService.facts(stack.getItem());
        source.sendSuccess(()->Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem())+" detected="+p.enabled()+" category="+p.weaponClass()
                +(f==null?"":" weight="+f.weight()+" damage="+f.damage()+" speed="+f.speed())
                +" STR="+p.strength()+" DEX="+p.dexterity()+" INT="+p.intelligence()+" FTH="+p.faith()+" ARC="+p.arcane()
                +" source="+p.source()+" reason="+p.debugReason()),false);
        if(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            var result=WeaponRequirementService.evaluate(player,stack);
            source.sendSuccess(()->Component.literal("missing="+result.missingAttributes()+" satisfied="+result.satisfied()+" multiplier="+result.damageMultiplier()+" innateAllowed="+result.weaponSkillAllowed()),false);
        }
        return 1;
    }
    public static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException(); var resolved=WeaponCombatProfileResolver.resolve(player.getMainHandItem());
        int arc=dev.maplesadventure.progression.PlayerAttributeService.get(player,dev.maplesadventure.progression.Attribute.ARCANE);
        var weight=resolved.statuses().weightClass()!=null?resolved.statuses().weightClass():dev.maplesadventure.progression.status.StatusWeaponWeightClass.archetype(resolved.requirements().weaponClass());
        source.sendSuccess(()->Component.literal("infusion="+resolved.infusion().id()+" weight="+weight+" ARC="+arc+" motion=1 (neutral preview)"),false);
        for(var c:resolved.statuses().components()) source.sendSuccess(()->Component.literal(c.type()+" base="+c.baseBuildup()+" policy="+c.policy()
                +" curve="+dev.maplesadventure.progression.status.StatusArcaneScalingCurve.evaluate(arc)+" scaling="+
                (c.policy()==dev.maplesadventure.progression.status.StatusArcaneScalingPolicy.FOLLOW_WEAPON_ARCANE?resolved.scaling().arcane():c.arcaneScaling())
                +" final="+c.amount(arc,resolved.scaling().arcane(),1)),false);
        return 1;
    }
    private static int damage(CommandSourceStack source,ItemStack stack) {
        var p=WeaponCombatProfileResolver.resolve(stack);
        source.sendSuccess(()->Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem())+" damage="+p.damage()),false);
        var state=source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player?dev.maplesadventure.progression.PlayerAttributeService.state(player):dev.maplesadventure.progression.PlayerAttributeState.defaultsState();
        var requirement=WeaponRequirementService.evaluate(state,p.requirements(),dev.maplesadventure.config.WeaponRequirementConfig.UNMET_MULTIPLIER.get());
        var bundle=WeaponAttackRatingCalculator.calculate(WeaponClassifier.baseAttack(stack),p.scaling(),p.damage(),state,requirement.damageMultiplier());
        source.sendSuccess(()->Component.literal("channels="+bundle.channels()+" totalAR="+bundle.totalAttackRating()+" nominal="+bundle.nominalMultiplier()+" efficiency="+bundle.requirementMultiplier()+" effective="+bundle.effectiveMultiplier()),false);
        return 1;
    }
    private static int scaling(CommandSourceStack source,ItemStack stack) {
        var resolved=WeaponCombatProfileResolver.resolve(stack);
        var p=resolved.scaling(); double base=WeaponClassifier.baseAttack(stack);
        var line=new StringBuilder(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).append(" weapon=").append(WeaponScalingService.isWeapon(stack))
                .append(" class=").append(p.weaponClass()).append(" base=").append(base).append(" source=").append(p.source()).append(" cap=").append(p.maxBonus());
        for(var a:WeaponRequirementProfile.ATTRIBUTES) line.append(' ').append(a.name()).append('=').append(p.get(a)).append('/').append(ScalingGrade.of(p.get(a)));
        for(int value:new int[]{5,20,40,60,99}) {
            var attributes=dev.maplesadventure.progression.PlayerAttributeState.defaultsState();
            for(var a:WeaponRequirementProfile.ATTRIBUTES) attributes=attributes.with(a,value,99);
            line.append(" AR").append(value).append('=').append(String.format(java.util.Locale.ROOT,"%.4f",WeaponAttackRatingCalculator.calculate(base,p,resolved.damage(),attributes,1).totalAttackRating()));
        }
        source.sendSuccess(()->Component.literal(line.toString()),false);
        return 1;
    }
    private static int infusionInfo(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var stack=source.getPlayerOrException().getMainHandItem();
        if(stack.isEmpty()) { source.sendFailure(Component.literal("Main hand is empty")); return 0; }
        var state=WeaponInfusionService.state(stack); var resolved=WeaponCombatProfileResolver.resolve(stack);
        source.sendSuccess(()->Component.literal("stored="+state.map(v->v.infusionId()+" v"+v.dataVersion()).orElse("<implicit normal>")
                +" runtime="+resolved.infusion().id()+" status="+resolved.infusion().status()+" eligible="+WeaponInfusionEligibilityService.eligibility(stack).allowed()),false);
        status(source); return damage(source,stack);
    }
    private static int setInfusion(CommandSourceStack source,net.minecraft.resources.ResourceLocation id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException(); var result=WeaponInfusionService.set(player,id);
        if(result!=WeaponInfusionService.SetResult.SUCCESS) { source.sendFailure(Component.literal("Infusion failed: "+result)); return 0; }
        source.sendSuccess(()->Component.literal("Set main-hand infusion to "+id),true); return 1;
    }
    private static int clearInfusion(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException(); if(!WeaponInfusionService.clear(player)) { source.sendFailure(Component.literal("Main hand is empty")); return 0; }
        source.sendSuccess(()->Component.literal("Cleared main-hand infusion (NORMAL)"),true); return 1;
    }
}
