package dev.maplesadventure.api.weapon;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Current stack infusion. The ID, not a translated label, is the logic contract. */
public record WeaponInfusionInfo(ResourceLocation id, State state, Optional<ResourceLocation> icon) {
    public enum State { NORMAL, APPLIED, UNKNOWN, INELIGIBLE }
    public WeaponInfusionInfo {
        Objects.requireNonNull(id); Objects.requireNonNull(state); Objects.requireNonNull(icon);
        if (id.toString().length() > 256 || icon.map(value -> value.toString().length() > 256).orElse(false))
            throw new IllegalArgumentException("Infusion ID bounds");
    }
}
