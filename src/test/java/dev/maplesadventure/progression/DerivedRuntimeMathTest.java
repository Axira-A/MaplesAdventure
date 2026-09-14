package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;

import dev.maplesadventure.progression.runtime.AttributeRuntimeSupport;
import dev.maplesadventure.progression.runtime.DerivedRuntimeResource;
import dev.maplesadventure.progression.runtime.PlayerResourceCheckpoint;
import dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot;
import dev.maplesadventure.progression.runtime.RuntimeResourceValue;
import dev.maplesadventure.progression.stats.StatImplementationState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DerivedRuntimeMathTest {
    @Test void resourceRatioIsPreservedInBothDirections() {
        double[] current = {0.0D};
        AttributeRuntimeSupport.preserveRatio(20.0D, 10.0D, 35.0D, value -> current[0] = value);
        assertEquals(17.5D, current[0], 0.000_001D);
        AttributeRuntimeSupport.preserveRatio(60.0D, 30.0D, 20.0D, value -> current[0] = value);
        assertEquals(10.0D, current[0], 0.000_001D);
    }

    @Test void serverRuntimeContextKeepsExternalModifierInLevelUpPreview() {
        PlayerAttributeState state = PlayerAttributeState.defaultsState().with(Attribute.VIGOR, 20, 99);
        RuntimeResourceSnapshot runtime = new RuntimeResourceSnapshot(
                new RuntimeResourceValue(35.0D, 40.0D, 1.0D, StatImplementationState.ACTIVE),
                RuntimeResourceValue.previewOnly(DerivedStatCalculator.mana(5)),
                RuntimeResourceValue.previewOnly(DerivedStatCalculator.stamina(5)));
        AttributeSnapshot baseline = new AttributeSnapshot(state, 20, 40.0D, 100.0D, 20.0D,
                AttributeProgression.costForNextLevel(20, 1.0D), runtime,
                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable());

        var preview = LevelUpPreviewCalculator.calculate(baseline, Map.of(Attribute.VIGOR, 1),
                10_000, 99, 1.0D);
        assertEquals(41.25D, preview.maxHealth(), 0.000_001D);
        assertEquals(StatImplementationState.ACTIVE,
                preview.characterStats().value(dev.maplesadventure.progression.stats.CharacterStat.MAX_HEALTH)
                        .implementation());
        assertEquals(5.0D, preview.characterStats()
                .value(dev.maplesadventure.progression.stats.CharacterStat.MAX_HEALTH)
                .breakdown().equipment(), 0.000_001D);
    }

    @Test void logoutCheckpointClampsAndRoundTripsResourceRatios() {
        PlayerResourceCheckpoint checkpoint = new PlayerResourceCheckpoint();
        checkpoint.put(DerivedRuntimeResource.MANA, 0.25D);
        checkpoint.put(DerivedRuntimeResource.STAMINA, 2.0D);
        var serialized = checkpoint.serializeNBT(null);

        PlayerResourceCheckpoint restored = new PlayerResourceCheckpoint();
        restored.deserializeNBT(null, serialized);
        assertEquals(0.25D, restored.ratio(DerivedRuntimeResource.MANA).orElseThrow(), 0.000_001D);
        assertEquals(1.0D, restored.ratio(DerivedRuntimeResource.STAMINA).orElseThrow(), 0.000_001D);
        assertTrue(restored.ratio(DerivedRuntimeResource.HEALTH).isEmpty());
    }
}
