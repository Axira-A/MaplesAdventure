package dev.maplesadventure.progression.stats;

public enum DamageDefenseType {
    PHYSICAL(CharacterStat.PHYSICAL_DEFENSE),
    SLASH(CharacterStat.SLASH_DEFENSE),
    STRIKE(CharacterStat.STRIKE_DEFENSE),
    PIERCE(CharacterStat.PIERCE_DEFENSE),
    MAGIC(CharacterStat.MAGIC_DEFENSE),
    FIRE(CharacterStat.FIRE_DEFENSE),
    LIGHTNING(CharacterStat.LIGHTNING_DEFENSE),
    ICE(CharacterStat.ICE_DEFENSE),
    HOLY(CharacterStat.HOLY_DEFENSE);

    private final CharacterStat stat;
    DamageDefenseType(CharacterStat stat) { this.stat = stat; }
    public CharacterStat stat() { return stat; }
    public dev.maplesadventure.progression.weapon.WeaponDamageChannel channel() {
        return dev.maplesadventure.progression.defense.DamageChannelMapping.channel(this);
    }
}
