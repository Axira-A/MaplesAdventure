package dev.maplesadventure.flask;

import dev.maplesadventure.client.bonfire.BonfireSubscreenMotion;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlaskRecoveryTest {
    @Test void sixIncrementsSumToEveryFrozenAmountExactlyOnce() {
        for (int potency = 0; potency <= 12; potency++) for (double total : new double[]{FlaskRules.health(potency), FlaskRules.mana(potency)}) {
            var clock = new FlaskRecoveryTimeline(total);
            double sum = 0;
            for (int tick = 1; tick <= 6; tick++) {
                assertFalse(clock.finished());
                double step = clock.nextAmount();
                assertEquals(total / 6, step, 1e-12);
                sum += step;
            }
            assertEquals(total, sum, 1e-12);
            assertTrue(clock.finished());
            assertEquals(0, clock.nextAmount());
        }
    }

    @Test void rejectsInvalidAmountsAndAllowsZero() {
        for (double bad : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, 1_000_001})
            assertThrows(IllegalArgumentException.class, () -> new FlaskRecoveryTimeline(bad));
        var zero = new FlaskRecoveryTimeline(0);
        for (int tick = 0; tick < 6; tick++) assertEquals(0, zero.nextAmount());
        assertTrue(zero.finished());
    }

    @Test void sharedMotionEntersFromRightAndCompletesExitOnce() {
        var time = new AtomicLong();
        var motion = new BonfireSubscreenMotion(time::get);
        assertEquals(0, motion.alpha()); assertEquals(24, motion.offset()); assertFalse(motion.interactive());
        time.set(160_000_000);
        assertEquals(.5, motion.alpha()); assertEquals(12, motion.offset());
        time.set(320_000_000);
        assertEquals(1, motion.alpha()); assertEquals(0, motion.offset()); assertTrue(motion.interactive());
        motion.beginExit(); assertFalse(motion.interactive());
        time.set(480_000_000);
        assertEquals(.5, motion.alpha()); assertEquals(12, motion.offset()); assertFalse(motion.claimCompletion());
        motion.beginExit(); // duplicate close cannot restart the timer
        time.set(640_000_000);
        assertEquals(0, motion.alpha()); assertEquals(24, motion.offset());
        assertTrue(motion.claimCompletion()); assertFalse(motion.claimCompletion());
    }

    @Test void closingDuringEntryHasNoJump() {
        var time = new AtomicLong();
        var motion = new BonfireSubscreenMotion(time::get);
        time.set(100_000_000);
        float alpha = motion.alpha(), offset = motion.offset();
        motion.beginExit();
        assertEquals(alpha, motion.alpha()); assertEquals(offset, motion.offset());
        time.set(420_000_000);
        assertEquals(0, motion.alpha()); assertEquals(24, motion.offset()); assertTrue(motion.claimCompletion());
    }
}
