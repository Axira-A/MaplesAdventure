package dev.maplesadventure.progression.encumbrance;

import net.minecraft.server.level.ServerPlayer;

/** Optional combat-mod boundary; common progression never loads Epic Fight classes. */
public interface EncumbranceCombatAdapter {
    void registerHooks();
    double currentEquipmentLoad(ServerPlayer player);
    DodgeMode currentDodgeMode(ServerPlayer player);
    boolean isDodgeAnimationActive(ServerPlayer player);
    void initializeAndRegister(ServerPlayer player, CombatSkillInitializationState migration);
    void apply(ServerPlayer player, EquipLoadRuntimeSnapshot snapshot, EncumbranceProfile profile,
               boolean allowDodgeSwitch);
}
