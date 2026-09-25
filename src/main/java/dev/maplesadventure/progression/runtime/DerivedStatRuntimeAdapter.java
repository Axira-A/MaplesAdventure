package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.progression.PlayerAttributeState;
import java.util.OptionalDouble;
import net.minecraft.server.level.ServerPlayer;

/** Optional integrations are loaded reflectively; this interface never references their classes. */
public interface DerivedStatRuntimeAdapter {
    DerivedRuntimeResource resource();
    RuntimeResourceValue refresh(ServerPlayer player, PlayerAttributeState state, DerivedStatRefreshReason reason);
    RuntimeResourceValue inspect(ServerPlayer player, PlayerAttributeState state);
    default OptionalDouble captureCurrentRatio(ServerPlayer player) { return OptionalDouble.empty(); }
    /** Exact runtime value for restore diagnostics; not a clamped or formula-derived preview. */
    default OptionalDouble currentValue(ServerPlayer player) { return OptionalDouble.empty(); }
    default void restoreCurrentRatio(ServerPlayer player, double ratio) {}
    default boolean consumeExact(ServerPlayer player,double amount) { return false; }
}
