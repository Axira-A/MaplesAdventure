package dev.maplesadventure.bonfire;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BonfireTransitionMathTest {
    @Test void menuFadesInAndVeilBecomesFullyTransparentAtTheRightEdge() {
        assertEquals(0, BonfireTransitionMath.menuAlpha(0));
        assertEquals(0.5F, BonfireTransitionMath.menuAlpha(160), 1e-6);
        assertEquals(1, BonfireTransitionMath.menuAlpha(320));
        assertEquals(1, BonfireTransitionMath.menuAlpha(1000));
        assertEquals(1, BonfireTransitionMath.veilOpacity(0));
        assertEquals(0, BonfireTransitionMath.veilOpacity(1));
        float previous = 1;
        for (int x = 0; x <= 100; x++) {
            float next = BonfireTransitionMath.veilOpacity(x / 100.0);
            assertTrue(next <= previous);
            previous = next;
        }
        assertEquals(0x00E5D0A5, BonfireTransitionMath.withOpacity(0xFFE5D0A5, 0));
        assertEquals(0xFFE5D0A5, BonfireTransitionMath.withOpacity(0xFFE5D0A5, 1));
    }
    @Test void encounterResetCommitIsDueOnlyOnceAtFullBlack() {
        assertFalse(BonfireTransitionMath.shouldCommit(BonfireSessionState.SITTING_DOWN, 9, 10, false));
        assertEquals(1, BonfireTransitionMath.fadeAlpha(10, 40, 10, 16), 1e-6);
        assertTrue(BonfireTransitionMath.shouldCommit(BonfireSessionState.SITTING_DOWN, 10, 10, false));
        assertFalse(BonfireTransitionMath.shouldCommit(BonfireSessionState.SITTING_DOWN, 11, 10, true));
        assertFalse(BonfireTransitionMath.shouldCommit(BonfireSessionState.ACTIVATING, 10, 10, false));
        assertFalse(BonfireTransitionMath.shouldCommit(BonfireSessionState.RESTING, 10, 10, false));
    }

    @Test void animatedAndFallbackFadeReturnToClearWorld() {
        for (int[] timing : new int[][] {{43, 14, 20}, {24, 10, 16}}) {
            int duration = timing[0], commit = timing[1], fadeIn = timing[2];
            assertEquals(0, BonfireTransitionMath.fadeAlpha(0, duration, commit, fadeIn), 1e-6);
            assertEquals(1, BonfireTransitionMath.fadeAlpha(commit, duration, commit, fadeIn), 1e-6);
            assertEquals(1, BonfireTransitionMath.fadeAlpha(fadeIn, duration, commit, fadeIn), 1e-6);
            assertEquals(0, BonfireTransitionMath.fadeAlpha(Math.min(duration, fadeIn + 8), duration, commit, fadeIn));
            assertEquals(0, BonfireTransitionMath.fadeAlpha(duration, duration, commit, fadeIn), 1e-6);
        }
    }

    @Test void sittingStartsBeforeBlackAndEndsInAClearWorld() {
        assertEquals(0, BonfireTransitionMath.fadeAlpha(5, 43, 14, 20));
        assertTrue(BonfireTransitionMath.fadeAlpha(7, 43, 14, 20) > 0);
        for (int tick = 14; tick <= 20; tick++)
            assertEquals(1, BonfireTransitionMath.fadeAlpha(tick, 43, 14, 20));
        assertEquals(0.5F, BonfireTransitionMath.fadeAlpha(24, 43, 14, 20));
        assertEquals(0, BonfireTransitionMath.fadeAlpha(28, 43, 14, 20));
        assertEquals(0, BonfireTransitionMath.fadeAlpha(40, 43, 14, 20));
    }
}
