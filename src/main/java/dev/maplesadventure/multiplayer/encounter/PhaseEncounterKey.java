package dev.maplesadventure.multiplayer.encounter;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import net.minecraft.resources.ResourceLocation;

public record PhaseEncounterKey(PhaseId phaseId, ResourceLocation encounterId) {}
