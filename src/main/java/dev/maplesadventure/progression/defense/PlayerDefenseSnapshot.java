package dev.maplesadventure.progression.defense;

import java.util.Map;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import dev.maplesadventure.progression.stats.StatBreakdown;

public record PlayerDefenseSnapshot(Map<WeaponDamageChannel, StatBreakdown> values) implements ChannelDefenseView {
    public PlayerDefenseSnapshot {
        values = Map.copyOf(values);
        for (var type : WeaponDamageChannel.values()) new ChannelDefense(values.get(type).total(), 0);
    }
    @Override public ChannelDefense channel(WeaponDamageChannel type) { return new ChannelDefense(values.get(type).total(), 0); }
    @Override public String source() { return "PLAYER_BUILD"; }
}
