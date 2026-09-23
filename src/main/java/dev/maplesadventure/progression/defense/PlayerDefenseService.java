package dev.maplesadventure.progression.defense;

import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.stats.DefenseCalculator;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** On-demand nine-value calculation from attributes and four actual armor slots. */
public final class PlayerDefenseService {
    public static PlayerDefenseSnapshot snapshot(ServerPlayer player) { return DefenseCalculator.snapshot(PlayerAttributeService.state(player),
            dev.maplesadventure.progression.armor.ArmorEquipmentService.snapshot(player)); }
    public static PlayerDefenseSnapshot preview(PlayerAttributeState attributes) { return DefenseCalculator.snapshot(attributes); }
    public static ChannelDefense channel(ServerPlayer player, WeaponDamageChannel type) { return snapshot(player).channel(type); }
    public static PlayerDefenseSnapshot refresh(ServerPlayer player) { return snapshot(player); }
    private PlayerDefenseService() {}
}
