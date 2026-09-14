package dev.maplesadventure.progression.stamina;

import java.util.EnumSet;

/** Server-owned transient stamina state. Nothing here is persisted across a player clone/restart. */
public final class StaminaRuntimeState {
    private double debt;
    private final EnumSet<StaminaRegenGate> gates = EnumSet.noneOf(StaminaRegenGate.class);

    public double debt() { return debt; }
    public void debt(double value) { debt = Math.clamp(value, 0.0D, StaminaPolicy.MAX_STAMINA_DEBT); }
    public boolean hasDebt() { return debt > 0.000_001D; }

    public void setGate(StaminaRegenGate gate, boolean closed) {
        if (closed) gates.add(gate); else gates.remove(gate);
    }

    public boolean regenBlocked() { return !gates.isEmpty(); }
    public void clearGates() { gates.clear(); }
    public void reset() { debt = 0.0D; gates.clear(); }
}
