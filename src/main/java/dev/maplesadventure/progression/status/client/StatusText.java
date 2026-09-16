package dev.maplesadventure.progression.status.client;
import java.util.*;
import dev.maplesadventure.progression.status.*;
import net.minecraft.network.chat.Component;
public final class StatusText {
    public static List<Component> lines(StatusBuildupSnapshot snapshot) {
        var lines=new ArrayList<Component>();
        snapshot.amounts().forEach((t,v)->{ if(v>0) lines.add(Component.translatable("status.maplesadventure.buildup",
                Component.translatable("status.maplesadventure."+t.id()),dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(v))); });
        return lines;
    }
    private StatusText() {}
}
