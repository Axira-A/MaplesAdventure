package dev.maplesadventure.integration.epicfight.progression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.runtime.AttributeRuntimeSupport;
import dev.maplesadventure.progression.runtime.DerivedRuntimeResource;
import dev.maplesadventure.progression.runtime.DerivedStatRefreshReason;
import dev.maplesadventure.progression.runtime.DerivedStatRuntimeAdapter;
import dev.maplesadventure.progression.runtime.RuntimeResourceValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import java.util.OptionalDouble;

/** Loaded reflectively only when Epic Fight 21.17.3.1 is present. */
public final class EpicFightStaminaAdapter implements DerivedStatRuntimeAdapter {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "progression_endurance_stamina");
    private static final double EPIC_FIGHT_BASELINE = 15.0D;

    @Override public DerivedRuntimeResource resource() { return DerivedRuntimeResource.STAMINA; }
    @Override public OptionalDouble currentValue(ServerPlayer player) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        return patch == null ? OptionalDouble.empty() : OptionalDouble.of(patch.getStamina());
    }

    @Override
    public RuntimeResourceValue refresh(ServerPlayer player, PlayerAttributeState state, DerivedStatRefreshReason reason) {
        var instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (instance == null || patch == null) throw new IllegalStateException("Epic Fight player stamina is not ready");
        double formula = resource().formula(state);
        return AttributeRuntimeSupport.apply(instance, MODIFIER_ID, formula, EPIC_FIGHT_BASELINE,
                patch::getStamina, value -> patch.setStamina((float) value));
    }

    @Override public RuntimeResourceValue inspect(ServerPlayer player, PlayerAttributeState state) {
        var instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        if (instance == null) throw new IllegalStateException("Epic Fight MAX_STAMINA is absent");
        return AttributeRuntimeSupport.inspect(instance, resource().formula(state));
    }

    @Override
    public OptionalDouble captureCurrentRatio(ServerPlayer player) {
        var instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (instance == null || patch == null) return OptionalDouble.empty();
        return AttributeRuntimeSupport.currentRatio(instance.getValue(), patch.getStamina());
    }

    @Override
    public void restoreCurrentRatio(ServerPlayer player, double ratio) {
        var instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (instance == null || patch == null) return;
        AttributeRuntimeSupport.restoreRatio(instance.getValue(), ratio, value -> patch.setStamina((float) value));
    }
}
