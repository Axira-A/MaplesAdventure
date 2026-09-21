package dev.maplesadventure.api.damage;

/** Stable typed-pressure channels. PHYSICAL is standard damage, not the sum of other physical types. */
public enum MaplesDamageChannel {
    /** Standard physical. */ PHYSICAL,
    /** Slashing. */ SLASH,
    /** Blunt. */ STRIKE,
    /** Piercing. */ PIERCE,
    /** Magic. */ MAGIC,
    /** Fire. */ FIRE,
    /** Lightning. */ LIGHTNING,
    /** Ice. */ ICE,
    /** Holy. */ HOLY
}
