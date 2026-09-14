package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.progression.upgrade.UpgradeAccessPolicy;
import org.junit.jupiter.api.Test;

class BonfireUpgradePolicyTest {
    @Test void onlySoloAndHostCanUpgrade() {
        assertTrue(UpgradeAccessPolicy.allows(PhaseRole.SOLO, true, false, false, false));
        assertTrue(UpgradeAccessPolicy.allows(PhaseRole.HOST, true, false, false, false));
        assertFalse(UpgradeAccessPolicy.allows(PhaseRole.COOPERATOR, true, false, false, false));
        assertFalse(UpgradeAccessPolicy.allows(PhaseRole.INVADER, true, false, false, false));
    }
    @Test void bossHostileDeathAndSpectatorAlwaysDeny() {
        for (PhaseRole role : PhaseRole.values()) {
            assertFalse(UpgradeAccessPolicy.allows(role, true, false, true, false));
            assertFalse(UpgradeAccessPolicy.allows(role, true, false, false, true));
            assertFalse(UpgradeAccessPolicy.allows(role, false, false, false, false));
            assertFalse(UpgradeAccessPolicy.allows(role, true, true, false, false));
        }
    }
    @Test void persistentStateRoundTripPreservesUpgradeWithoutPersistingLevel() {
        var state = PlayerAttributeState.defaultsState().with(Attribute.VIGOR, 8, 99);
        var tag = state.serializeNBT(null);
        assertFalse(tag.contains("Level"));
        var loaded = PlayerAttributeState.defaultsState();
        loaded.deserializeNBT(null, tag);
        assertEquals(state.values(), loaded.values());
        assertEquals(8, AttributeProgression.level(loaded));
    }
}
