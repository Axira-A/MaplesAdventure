package dev.maplesadventure.progression.weapon;

import java.util.List;

/** One event's channel arithmetic; finalDamage is before Vanilla absorption hearts. */
public record WeaponDamageResolution(double originalDamage, double nominalDamage, List<ChannelResult> channelResults,
        double qualifiedDamage, double requirementMultiplier, double finalDamage) {
    public WeaponDamageResolution { channelResults = List.copyOf(channelResults); }
    public record ChannelResult(WeaponDamageChannel channel, double attackRating, double share, double incomingDamage,
            double defense, double absorption, double defenseMultiplier, double finalDamage) {}
}
