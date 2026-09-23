package dev.maplesadventure.api.weapon;

import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.MaplesStatusType;
import java.util.*;

/** Server-authoritative player evaluation; status buildup assumes motion value 1.0. */
public record WeaponEvaluationView(WeaponProfileView profile, boolean requirementsSatisfied,
        Map<MaplesWeaponAttribute, Integer> missingRequirements, double requirementDamageMultiplier,
        boolean weaponSkillAllowed, double totalAttackRating,
        Map<MaplesDamageChannel, WeaponAttackChannelView> channels,
        Map<MaplesStatusType, Double> statusBuildupAtMotionOne) {
    public WeaponEvaluationView {
        Objects.requireNonNull(profile);
        missingRequirements = Map.copyOf(missingRequirements);
        channels = Map.copyOf(channels);
        statusBuildupAtMotionOne = Map.copyOf(statusBuildupAtMotionOne);
        if (!Double.isFinite(requirementDamageMultiplier) || requirementDamageMultiplier < .1 || requirementDamageMultiplier > 1 ||
                !Double.isFinite(totalAttackRating) || totalAttackRating < 0)
            throw new IllegalArgumentException("Weapon evaluation bounds");
    }
}
