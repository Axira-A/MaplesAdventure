package dev.maplesadventure.multiplayer.phase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.maplesadventure.multiplayer.phase.client.ClientPhaseState;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PhaseCoreTest {
    @AfterEach void clearClientMirror() { ClientPhaseState.clear(); }

    @Test
    void soloPhaseIsStableAndUniquePerPlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertEquals(PhaseId.solo(first), PhaseId.solo(first));
        assertNotEquals(PhaseId.solo(first), PhaseId.solo(second));
        assertEquals(PhaseRole.SOLO, PlayerPhaseState.solo(first).role());
    }

    @Test
    void clientMirrorFailsClosedAndAppliesServerAuthoredUpdates() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertNotEquals(ClientPhaseState.state(first).phaseId(), ClientPhaseState.state(second).phaseId());

        PlayerPhaseState shared = new PlayerPhaseState(PhaseId.solo(first), PhaseRole.SOLO);
        ClientPhaseState.replace(Map.of(first, shared, second, shared));
        assertEquals(ClientPhaseState.state(first), ClientPhaseState.state(second));

        ClientPhaseState.remove(second);
        assertNotEquals(ClientPhaseState.state(first).phaseId(), ClientPhaseState.state(second).phaseId());
    }
}
