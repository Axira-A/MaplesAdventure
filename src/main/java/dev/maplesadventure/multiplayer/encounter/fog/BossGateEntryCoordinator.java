package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import net.minecraft.server.MinecraftServer;

/** Stable hand-off point for the future invasion system. */
public interface BossGateEntryCoordinator {
    boolean endActiveInvasion(MinecraftServer server, PhaseId phase, Reason reason);

    BossGateEntryCoordinator NONE = (server, phase, reason) -> true;
    enum Reason { BOSS_GATE_ENTRY }
}
