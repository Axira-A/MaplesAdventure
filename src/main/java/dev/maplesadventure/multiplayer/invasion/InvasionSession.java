package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.UUID;

public final class InvasionSession {
    public static final UUID NO_COOPERATOR = new UUID(0L, 0L);
    private final UUID sessionId;
    private final UUID hostUuid;
    private final UUID cooperatorUuid;
    private final UUID invaderUuid;
    private final PhaseId hostPhaseId;
    private final HostileSessionType type;
    private final long createdAt;
    private long startedAt;
    private long materializeUntilTick;
    private InvasionSessionState state;

    public InvasionSession(UUID sessionId, UUID hostUuid, UUID cooperatorUuid, UUID invaderUuid,
                           PhaseId hostPhaseId, HostileSessionType type, long createdAt) {
        this.sessionId = sessionId;
        this.hostUuid = hostUuid;
        this.cooperatorUuid = cooperatorUuid;
        this.invaderUuid = invaderUuid;
        this.hostPhaseId = hostPhaseId;
        this.type = type;
        this.createdAt = createdAt;
        this.state = InvasionSessionState.PREPARING;
    }

    public UUID sessionId() { return sessionId; }
    public UUID hostUuid() { return hostUuid; }
    public UUID cooperatorUuid() { return cooperatorUuid; }
    public UUID invaderUuid() { return invaderUuid; }
    public PhaseId hostPhaseId() { return hostPhaseId; }
    public HostileSessionType type() { return type; }
    public boolean hasCooperator() { return !NO_COOPERATOR.equals(cooperatorUuid); }
    public long createdAt() { return createdAt; }
    public long startedAt() { return startedAt; }
    public long materializeUntilTick() { return materializeUntilTick; }
    public InvasionSessionState state() { return state; }
    void materialize(long nowTick, int durationTicks) {
        state = InvasionSessionState.MATERIALIZING;
        startedAt = System.currentTimeMillis();
        materializeUntilTick = nowTick + durationTicks;
    }
    void activate() { state = InvasionSessionState.ACTIVE; }
    void setState(InvasionSessionState state) { this.state = state; }
}
