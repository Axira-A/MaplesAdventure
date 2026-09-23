package dev.maplesadventure.bonfire;

import static org.junit.jupiter.api.Assertions.*;

import dev.maplesadventure.multiplayer.phase.PhaseRole;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BonfireCoreTest {
    private static BonfireRef ref(int x, UUID generation) {
        return new BonfireRef(ResourceLocation.parse("minecraft:overworld"), new BlockPos(x, 64, 3), generation);
    }
    @Test void placementIdentityIncludesDimensionPositionAndGeneration() {
        UUID id = UUID.randomUUID();
        BonfireRef original = ref(1, id);
        assertEquals(original, BonfireRef.load(original.save()).orElseThrow());
        assertNotEquals(original, ref(2, id));
        assertNotEquals(original, ref(1, UUID.randomUUID()));
        assertNotEquals(original, new BonfireRef(ResourceLocation.parse("minecraft:the_nether"), original.pos(), id));
    }
    @Test void invalidEntriesAreDiscardedAndBounded() {
        assertTrue(BonfireRef.load(new CompoundTag()).isEmpty());
        CompoundTag malformed = ref(1, UUID.randomUUID()).save();
        malformed.putString("Generation", "not-a-uuid");
        assertTrue(BonfireRef.load(malformed).isEmpty());
        malformed = ref(1, UUID.randomUUID()).save();
        malformed.remove("X");
        assertTrue(BonfireRef.load(malformed).isEmpty());
        PlayerBonfireState state = new PlayerBonfireState();
        for (int i = 0; i < PlayerBonfireState.MAX_ACTIVATED; i++) assertTrue(state.activate(ref(i, UUID.randomUUID())));
        assertFalse(state.activate(ref(9000, UUID.randomUUID())));
        assertEquals(PlayerBonfireState.MAX_ACTIVATED, state.activated().size());
    }
    @Test void activationIsPerPlayerAndDoesNotSetRestPoint() {
        BonfireRef ref = ref(4, UUID.randomUUID());
        PlayerBonfireState a = new PlayerBonfireState(), b = new PlayerBonfireState();
        assertTrue(a.activate(ref));
        assertTrue(a.isActivated(ref));
        assertFalse(b.isActivated(ref));
        assertTrue(a.lastRested().isEmpty());
        assertTrue(a.rest(ref, 42));
        assertEquals(ref, a.lastRested().orElseThrow().ref());
        assertTrue(b.lastRested().isEmpty());
        PlayerBonfireState loaded = new PlayerBonfireState();
        loaded.deserializeNBT(null, a.serializeNBT(null));
        assertEquals(a.activated(), loaded.activated());
        assertEquals(a.lastRested(), loaded.lastRested());
    }
    @Test void accessPolicyDoesNotTreatForeignSessionsAsOwners() {
        assertTrue(BonfireAccessPolicy.allows(PhaseRole.SOLO, true, false, false, false));
        assertTrue(BonfireAccessPolicy.allows(PhaseRole.HOST, true, false, false, false));
        for (PhaseRole role : PhaseRole.values()) {
            assertFalse(BonfireAccessPolicy.allows(role, false, false, false, false));
            assertFalse(BonfireAccessPolicy.allows(role, true, true, false, false));
            assertFalse(BonfireAccessPolicy.allows(role, true, false, true, false));
            assertFalse(BonfireAccessPolicy.allows(role, true, false, false, true));
        }
        assertFalse(BonfireAccessPolicy.allows(PhaseRole.COOPERATOR, true, false, false, false));
        assertFalse(BonfireAccessPolicy.allows(PhaseRole.INVADER, true, false, false, false));
    }
    @Test void onlyImplementedLevelUpFeatureIsRuntimeVisible() {
        assertEquals(BonfireFeature.LEVEL_UP, BonfireFeature.byId("level_up").orElseThrow());
        assertTrue(BonfireFeature.byId("missing").isEmpty());
        assertEquals("spell_memory", BonfireFeature.SPELL_MEMORY.id());
    }
}
