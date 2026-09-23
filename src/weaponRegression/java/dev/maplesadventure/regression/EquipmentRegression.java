package dev.maplesadventure.regression;

import dev.maplesadventure.api.armor.*;
import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesResistanceType;
import dev.maplesadventure.api.weapon.*;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.armor.*;
import dev.maplesadventure.progression.defense.PlayerDefenseService;
import dev.maplesadventure.progression.stats.*;
import dev.maplesadventure.progression.status.*;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import static dev.maplesadventure.regression.PlayerDefenseRegression.check;

/** Dedicated-server armor contract probe; source set is opt-in and excluded from the JAR. */
public final class EquipmentRegression {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(
                Commands.literal("equipmentregression").requires(source -> source.hasPermission(2)).executes(context -> run(context.getSource()))));
    }
    private static int run(net.minecraft.commands.CommandSourceStack command) {
        var player = PlayerDefenseRegression.player(command.getLevel(), "EquipmentTarget");
        var chest = Items.LEATHER_CHESTPLATE.getDefaultInstance();
        var diamond = Items.DIAMOND_CHESTPLATE.getDefaultInstance();
        check(MaplesArmorApi.query(diamond).isEmpty(), "unprofiled diamond remains Maples zero");
        check(MaplesArmorApi.query(chest).orElseThrow().channels().get(MaplesDamageChannel.HOLY)==18,
                "public item query holy 18");
        double baseHoly = PlayerDefenseService.snapshot(player).channel(WeaponDamageChannel.HOLY).defense();
        double baseRobust = StatusResistanceService.resolve(player,StatusEffectType.BLEED).threshold();
        player.setItemSlot(EquipmentSlot.MAINHAND,chest);
        check(PlayerDefenseService.snapshot(player).channel(WeaponDamageChannel.HOLY).defense()==baseHoly,
                "held chestplate does not contribute");
        player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST,chest);
        check(PlayerDefenseService.snapshot(player).channel(WeaponDamageChannel.HOLY).defense()==baseHoly+18,
                "equipped holy defense is effective");
        check(StatusResistanceService.resolve(player,StatusEffectType.BLEED).threshold()==baseRobust+12,
                "equipped robustness affects bleed");
        check(StatusResistanceService.resolve(player,StatusEffectType.FROSTBITE).threshold()==baseRobust+12,
                "equipped robustness affects frostbite");
        var view=MaplesArmorApi.equipped(player).orElseThrow();
        check(view.totalChannels().get(MaplesDamageChannel.HOLY)==18 &&
                view.totalResistances().get(MaplesResistanceType.ROBUSTNESS)==12 &&
                view.slots().containsKey(EquipmentSlot.CHEST),"public equipped snapshot");
        var stats=CharacterStatsService.snapshot(player);
        check(stats.value(CharacterStat.HOLY_DEFENSE).breakdown().equipment()==18 &&
                stats.value(CharacterStat.ROBUSTNESS).breakdown().equipment()==12,"character breakdown matches runtime");
        player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        check(PlayerDefenseService.snapshot(player).channel(WeaponDamageChannel.HOLY).defense()==baseHoly,
                "unequip restores original defense");
        check(StatusResistanceService.resolve(player,StatusEffectType.BLEED).threshold()==baseRobust,
                "unequip restores original resistance");
        var blade=Items.STONE_SWORD.getDefaultInstance();
        var profile=MaplesWeaponApi.query(blade).orElseThrow();
        check(profile.requirements().requirements().get(MaplesWeaponAttribute.STRENGTH)==12
                && profile.requirements().requirements().get(MaplesWeaponAttribute.FAITH)==24
                && profile.damageComponents().size()==2,"public split Holy weapon profile: requirements="
                +profile.requirements().requirements()+" damage="+profile.damageComponents());
        var strengthReady=PlayerAttributeState.defaultsState().with(Attribute.STRENGTH,12,99);
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,strengthReady);
        var underqualified=MaplesWeaponApi.evaluate(player,blade).orElseThrow();
        check(!underqualified.requirementsSatisfied() && underqualified.missingRequirements().get(MaplesWeaponAttribute.FAITH)==19,
                "Faith requirement enforced");
        var lowerHoly=underqualified.channels().get(MaplesDamageChannel.HOLY).total();
        var lowerPhysical=underqualified.channels().get(MaplesDamageChannel.PHYSICAL).total();
        player.setData(ProgressionAttachments.PLAYER_ATTRIBUTES,strengthReady.with(Attribute.FAITH,24,99));
        var qualified=MaplesWeaponApi.evaluate(player,blade).orElseThrow();
        check(qualified.requirementsSatisfied() && qualified.channels().get(MaplesDamageChannel.HOLY).total()>lowerHoly,
                "Faith increases only the Holy component");
        check(Math.abs(qualified.channels().get(MaplesDamageChannel.PHYSICAL).total()-lowerPhysical)<1e-9,
                "Faith does not leak into physical component");
        command.sendSuccess(() -> Component.literal("Equipment regression PASS"),true);
        return 1;
    }
    private EquipmentRegression() {}
}
