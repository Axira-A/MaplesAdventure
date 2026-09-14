package dev.maplesadventure.progression.encumbrance;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Migration markers only; learned skills remain owned and persisted by Epic Fight. */
public final class CombatSkillInitializationState implements INBTSerializable<CompoundTag> {
    private int vanillaDodgeVersion;
    private int nightfallDodgeVersion;
    public int vanillaDodgeVersion() { return vanillaDodgeVersion; }
    public int nightfallDodgeVersion() { return nightfallDodgeVersion; }
    public void markVanilla(int version) { vanillaDodgeVersion = Math.max(vanillaDodgeVersion, version); }
    public void markNightfall(int version) { nightfallDodgeVersion = Math.max(nightfallDodgeVersion, version); }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 1);
        tag.putInt("VanillaDodgeVersion", vanillaDodgeVersion);
        tag.putInt("NightfallDodgeVersion", nightfallDodgeVersion);
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        vanillaDodgeVersion = Math.max(0, tag.getInt("VanillaDodgeVersion"));
        nightfallDodgeVersion = Math.max(0, tag.getInt("NightfallDodgeVersion"));
    }
}
