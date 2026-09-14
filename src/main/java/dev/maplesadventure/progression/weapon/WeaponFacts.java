package dev.maplesadventure.progression.weapon;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
public record WeaponFacts(WeaponRequirementArchetype archetype, String source, String category,
        double weight, double damage, double speed, Map<ResourceLocation,Double> affinity) {
    public WeaponFacts { affinity=Map.copyOf(affinity); }
}
