package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** @deprecated Sealed-room flood fill was retired; use {@link FogGateComponentScanner}. */
@Deprecated(forRemoval = false)
public final class FogGateRoomScanner {
    public static Result scan(ServerLevel level, EncounterDefinition encounter, BlockPos selectedGate) {
        FogGateComponentScanner.Result result = FogGateComponentScanner.scan(level, encounter, selectedGate);
        return new Result(result.valid(), result.reason(), result.definition());
    }
    public record Result(boolean valid, String reason, FogGateDefinition definition) {}
    private FogGateRoomScanner() {}
}
