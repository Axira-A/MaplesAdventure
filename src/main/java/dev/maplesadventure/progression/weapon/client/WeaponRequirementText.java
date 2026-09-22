package dev.maplesadventure.progression.weapon.client;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class WeaponRequirementText {
    public static Component status(boolean met) { return Component.translatable("screen.maplesadventure.weapon."+(met?"qualified":"unqualified")); }
    public static Component skill(boolean allowed) { return Component.translatable("screen.maplesadventure.weapon."+(allowed?"skill_allowed":"skill_blocked")); }
    public static List<Component> lines(WeaponLoadoutSnapshot.View v) {
        List<Component> lines=new ArrayList<>();
        lines.add(Component.translatable("screen.maplesadventure.weapon."+(v.offhand()?"offhand":"mainhand"),v.held().name()));
        if (v.held().handState() != WeaponLoadoutSnapshot.HandState.WEAPON) {
            lines.add(Component.translatable("screen.maplesadventure.weapon." +
                    (v.held().handState() == WeaponLoadoutSnapshot.HandState.EMPTY ? "empty_hand" : "no_weapon")));
            return List.copyOf(lines);
        }
        lines.add(Component.translatable("screen.maplesadventure.weapon.infusion",v.held().infusion().displayName()));
        if(v.held().weapon()) lines.addAll(WeaponAttackText.lines(v.attack()));
        lines.addAll(dev.maplesadventure.progression.status.client.StatusText.lines(v.statuses()));
        for(var a:WeaponRequirementProfile.ATTRIBUTES) if(v.held().profile().get(a)>0)
            lines.add(Component.translatable("screen.maplesadventure.weapon.current_requirement",a.displayName(),v.attributes().get(a),v.held().profile().get(a))
                .withStyle(v.attributes().get(a)>=v.held().profile().get(a)?net.minecraft.ChatFormatting.GRAY:net.minecraft.ChatFormatting.RED));
        lines.add(status(v.result().satisfied()));
        lines.add(Component.translatable("screen.maplesadventure.weapon.efficiency",Math.round(v.result().damageMultiplier()*100)));
        lines.add(skill(v.result().weaponSkillAllowed()));
        return lines;
    }
    private WeaponRequirementText() {}
}
