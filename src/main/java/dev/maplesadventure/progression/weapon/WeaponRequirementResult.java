package dev.maplesadventure.progression.weapon;
import dev.maplesadventure.progression.Attribute;
import java.util.Map;
public record WeaponRequirementResult(boolean satisfied, Map<Attribute,Integer> missingAttributes,
        double damageMultiplier, boolean weaponSkillAllowed) {
    public WeaponRequirementResult { missingAttributes = Map.copyOf(missingAttributes); }
}
