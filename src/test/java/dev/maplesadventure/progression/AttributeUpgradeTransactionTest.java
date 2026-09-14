package dev.maplesadventure.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributeUpgradeTransactionTest {
    private static final class Memory implements AttributeUpgradeTransaction.Writer {
        int xp = 1000, writes, syncs;
        PlayerAttributeState state = PlayerAttributeState.defaultsState();
        boolean failWrite, failSync;
        public void experience(int points) { xp = points; }
        public void attributes(PlayerAttributeState value) {
            if (failWrite) { failWrite = false; throw new IllegalStateException("injected attachment failure"); }
            state = value; writes++;
        }
        public void sync() {
            syncs++;
            if (failSync) { failSync = false; throw new IllegalStateException("injected sync failure"); }
        }
    }
    @Test void batchCommitsOnce() {
        var memory = new Memory();
        AttributeUpgradeTransaction.commit(memory, 1000, memory.state, 618, memory.state.with(Attribute.VIGOR, 8, 99));
        assertEquals(618, memory.xp);
        assertEquals(8, memory.state.get(Attribute.VIGOR));
        assertEquals(1, memory.writes);
        assertEquals(1, memory.syncs);
    }
    @Test void attachmentFailureRollsBackBothValues() {
        var memory = new Memory(); memory.failWrite = true;
        assertThrows(IllegalStateException.class, () -> AttributeUpgradeTransaction.commit(
                memory, 1000, memory.state, 618, memory.state.with(Attribute.VIGOR, 8, 99)));
        assertEquals(1000, memory.xp);
        assertEquals(5, memory.state.get(Attribute.VIGOR));
    }
    @Test void syncFailureRollsBackAndResyncs() {
        var memory = new Memory(); memory.failSync = true;
        assertThrows(IllegalStateException.class, () -> AttributeUpgradeTransaction.commit(
                memory, 1000, memory.state, 618, memory.state.with(Attribute.VIGOR, 8, 99)));
        assertEquals(1000, memory.xp);
        assertEquals(5, memory.state.get(Attribute.VIGOR));
        assertEquals(2, memory.syncs);
    }
}
