package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.stats.*;
import org.junit.jupiter.api.Test;

class StatPreviewPriorityTest {
    @Test void changedStatsPrecedeStableCoreAndUnavailableRowsAreOmitted() {
        PlayerAttributeState before = PlayerAttributeState.defaultsState();
        PlayerAttributeState after = before.with(Attribute.VIGOR, 8, 99);
        var rows = StatPreviewPriority.select(CharacterStatsService.preview(before),
                CharacterStatsService.preview(after), 7);
        assertEquals(CharacterStat.MAX_HEALTH, rows.getFirst().stat());
        assertTrue(rows.getFirst().changed());
        int firstUnchanged = -1;
        for (int i = 0; i < rows.size(); i++) {
            assertNotEquals(StatImplementationState.UNAVAILABLE, rows.get(i).preview().implementation());
            if (!rows.get(i).changed() && firstUnchanged < 0) firstUnchanged = i;
            if (firstUnchanged >= 0) assertFalse(rows.get(i).changed());
        }
        assertTrue(rows.stream().anyMatch(row -> row.stat() == CharacterStat.PHYSICAL_DEFENSE));
        assertFalse(rows.stream().anyMatch(row -> row.stat() == CharacterStat.MAIN_HAND_ATTACK));
    }
}
