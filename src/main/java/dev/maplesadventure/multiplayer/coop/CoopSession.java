package dev.maplesadventure.multiplayer.coop;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.UUID;

public final class CoopSession {
    private final UUID sessionId;
    private final UUID hostUuid;
    private final UUID cooperatorUuid;
    private final PhaseId phaseId;
    private final long createdAt;
    private CoopSessionState state = CoopSessionState.ACTIVE;

    public CoopSession(UUID sessionId, UUID hostUuid, UUID cooperatorUuid, PhaseId phaseId, long createdAt) {
        this.sessionId = sessionId;
        this.hostUuid = hostUuid;
        this.cooperatorUuid = cooperatorUuid;
        this.phaseId = phaseId;
        this.createdAt = createdAt;
    }

    public UUID sessionId() { return sessionId; }
    public UUID hostUuid() { return hostUuid; }
    public UUID cooperatorUuid() { return cooperatorUuid; }
    public PhaseId phaseId() { return phaseId; }
    public long createdAt() { return createdAt; }
    public CoopSessionState state() { return state; }
    void setState(CoopSessionState state) { this.state = state; }
}
