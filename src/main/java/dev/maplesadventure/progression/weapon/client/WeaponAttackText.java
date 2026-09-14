package dev.maplesadventure.progression.weapon.client;

import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.*;

public final class WeaponAttackText {
    public static String number(double value) { return String.format(Locale.ROOT,"%.1f",value); }
    public static Component rating(WeaponAttackSnapshot s) {
        return Component.translatable("screen.maplesadventure.weapon.total_ar",number(s.attackRating()));
    }
    public static List<Component> lines(WeaponAttackSnapshot s) {
        List<Component> lines=new ArrayList<>();
        for(var c:s.damageProfile().components()) {
            var ar=s.bundle().channels().get(c.channel()); if(ar==null) continue;
            lines.add(Component.translatable("screen.maplesadventure.weapon.channel_ar",c.channel().displayName(),number(ar.base()),number(ar.scalingBonus()),number(ar.attackRating())).withStyle(ChatFormatting.GOLD));
            var profile=c.scaling(s.profile());
            for(var a:WeaponRequirementProfile.ATTRIBUTES) if(profile.enabled()&&profile.get(a)>0)
                lines.add(Component.translatable("screen.maplesadventure.weapon.scaling_grade",a.displayName(),ScalingGrade.of(profile.get(a)).display()).withStyle(ChatFormatting.GRAY));
        }
        lines.add(rating(s).copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("screen.maplesadventure.weapon.scaling_bonus",number(s.effectiveScaling()*100)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.maplesadventure.weapon.ar_note").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }
    public static List<Component> contributions(PlayerAttributeState state,WeaponScalingProfile profile) {
        List<Component> result=new ArrayList<>();
        for(var a:WeaponRequirementProfile.ATTRIBUTES) if(profile.enabled() && profile.get(a)>0)
            result.add(Component.translatable("screen.maplesadventure.weapon.contribution",a.displayName(),state.get(a),
                    number(WeaponAttackRatingCalculator.contribution(state,profile,a)*100)).withStyle(ChatFormatting.DARK_GRAY));
        return result;
    }
    private WeaponAttackText() {}
}
