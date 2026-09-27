package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.FlaskSnapshot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Serialized attachment only. Normalization is shared by NBT migration and service writes. */
public final class FlaskState implements INBTSerializable<CompoundTag> {
    public static final int DATA_VERSION = 1;
    private FlaskSnapshot value = normalize(4, 0, 4, 4, 0, 0, false);
    private boolean initialized;
    public FlaskState() {}
    public FlaskState(FlaskSnapshot value) { this.value = value; initialized = true; }
    public boolean initialized() { return initialized; }
    public FlaskSnapshot value() { return value; }
    public static FlaskSnapshot initial(boolean mana) { return normalize(4, 0, mana ? 3 : 4, mana ? 3 : 4, mana ? 1 : 0, mana ? 1 : 0, mana); }
    public static FlaskSnapshot normalize(int capacity, int potency, int red, int redLeft, int blue, int blueLeft, boolean mana) {
        capacity = Math.clamp(capacity, 4, 14);
        potency = Math.clamp(potency, 0, 12);
        red = Math.clamp(red, 0, capacity);
        // Crimson is the canonical allocation; malformed blue values cannot create extra charges.
        blue = capacity - red;
        redLeft = Math.clamp(redLeft, 0, red);
        blueLeft = Math.clamp(blueLeft, 0, blue);
        if (!mana) { redLeft += blueLeft; red = capacity; blue = 0; blueLeft = 0; }
        return new FlaskSnapshot(capacity, potency, red, redLeft, blue, blueLeft);
    }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DATA_VERSION); tag.putBoolean("Initialized", initialized);
        tag.putInt("Capacity", value.totalCapacity()); tag.putInt("Potency", value.potencyLevel());
        tag.putInt("CrimsonAllocated", value.crimsonAllocated()); tag.putInt("CrimsonRemaining", value.crimsonRemaining());
        tag.putInt("AshenAllocated", value.ashenAllocated()); tag.putInt("AshenRemaining", value.ashenRemaining());
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        // Any existing saved record is an old player: never silently replace its 4/0 with 3/1.
        initialized = true;
        value = normalize(tag.contains("Capacity") ? tag.getInt("Capacity") : 4, tag.getInt("Potency"),
                tag.contains("CrimsonAllocated") ? tag.getInt("CrimsonAllocated") : 4,
                tag.getInt("CrimsonRemaining"), tag.getInt("AshenAllocated"), tag.getInt("AshenRemaining"), true);
    }
}
