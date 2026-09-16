package dev.maplesadventure.progression.defense;

import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Read-only target domain. Player builds never acquire enemy profile attachments. */
public interface ChannelDefenseView {
    ChannelDefense channel(WeaponDamageChannel channel);
    String source();
}
