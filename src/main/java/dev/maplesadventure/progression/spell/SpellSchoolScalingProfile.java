package dev.maplesadventure.progression.spell;

import com.google.gson.JsonObject;
import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.OffensiveScalingCurve;
import dev.maplesadventure.progression.PlayerAttributeState;
import net.minecraft.resources.ResourceLocation;

/** Server datapack policy; no third-party classes or damage pipeline. */
public record SpellSchoolScalingProfile(ResourceLocation schoolId, double intelligenceWeight,
        double faithWeight, double arcaneWeight, double maxProgressionBonus) {
    public SpellSchoolScalingProfile {
        if (schoolId == null || !valid(intelligenceWeight) || !valid(faithWeight) || !valid(arcaneWeight)
                || !valid(maxProgressionBonus) || maxProgressionBonus > 10
                || Math.abs(intelligenceWeight + faithWeight + arcaneWeight - 1) > 0.000001)
            throw new IllegalArgumentException("Weights must be finite, nonnegative and sum to 1; max_bonus must be 0..10");
    }
    private static boolean valid(double value) { return Double.isFinite(value) && value >= 0; }
    public double bonus(PlayerAttributeState state) {
        return maxProgressionBonus * (OffensiveScalingCurve.evaluate(state.get(Attribute.INTELLIGENCE)) * intelligenceWeight
                + OffensiveScalingCurve.evaluate(state.get(Attribute.FAITH)) * faithWeight
                + OffensiveScalingCurve.evaluate(state.get(Attribute.ARCANE)) * arcaneWeight);
    }
    public static SpellSchoolScalingProfile parse(JsonObject json) {
        for (String key : json.keySet()) if (!java.util.Set.of("school", "weights", "max_bonus").contains(key))
            throw new IllegalArgumentException("Unknown profile field: " + key);
        JsonObject weights = json.getAsJsonObject("weights");
        for (String key : weights.keySet()) if (!java.util.Set.of("intelligence", "faith", "arcane").contains(key))
            throw new IllegalArgumentException("Unknown scaling attribute: " + key);
        return new SpellSchoolScalingProfile(ResourceLocation.parse(json.get("school").getAsString()),
                number(weights, "intelligence", 0), number(weights, "faith", 0), number(weights, "arcane", 0),
                number(json, "max_bonus", .75));
    }
    private static double number(JsonObject json, String key, double fallback) {
        if (!json.has(key)) return fallback;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber())
            throw new IllegalArgumentException("Expected number: " + key);
        return json.get(key).getAsDouble();
    }
}
