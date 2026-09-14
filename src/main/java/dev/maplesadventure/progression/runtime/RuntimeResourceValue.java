package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.progression.stats.CharacterStatValue;
import dev.maplesadventure.progression.stats.StatBreakdown;
import dev.maplesadventure.progression.stats.StatImplementationState;

/**
 * Server-authored context needed to preview a progression change without losing external modifiers.
 * addValueScale is the product of all ADD_MULTIPLIED_TOTAL modifiers on the backing attribute.
 */
public record RuntimeResourceValue(double formulaValue, double runtimeValue, double addValueScale,
                                   StatImplementationState implementation) {
    public RuntimeResourceValue {
        if (!Double.isFinite(formulaValue) || !Double.isFinite(runtimeValue)
                || !Double.isFinite(addValueScale) || addValueScale <= 0.0D || implementation == null) {
            throw new IllegalArgumentException("Invalid runtime resource value");
        }
    }

    public static RuntimeResourceValue previewOnly(double formulaValue) {
        return new RuntimeResourceValue(formulaValue, formulaValue, 1.0D,
                StatImplementationState.PREVIEW_ONLY);
    }

    public CharacterStatValue project(double newFormula, double progressionBaseline) {
        if (implementation != StatImplementationState.ACTIVE) {
            return CharacterStatValue.previewOnly(new StatBreakdown(progressionBaseline,
                    newFormula - progressionBaseline, 0.0D, 0.0D));
        }
        double projected = runtimeValue + (newFormula - formulaValue) * addValueScale;
        // Minecraft does not expose a reliable equipment/effect provenance for arbitrary modifiers.
        // Keep the exact external total in one honest bucket instead of guessing from modifier ids.
        return CharacterStatValue.active(new StatBreakdown(progressionBaseline,
                newFormula - progressionBaseline, projected - newFormula, 0.0D));
    }
}
