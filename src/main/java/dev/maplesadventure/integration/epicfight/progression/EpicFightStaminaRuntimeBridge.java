package dev.maplesadventure.integration.epicfight.progression;

import dev.maplesadventure.progression.stamina.StaminaRuntimeBridge;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Epic Fight is retained as the synced stamina container/HUD and regen-multiplier provider. */
public final class EpicFightStaminaRuntimeBridge implements StaminaRuntimeBridge {
    @Override
    public boolean available(ServerPlayer player) {
        return EpicFightCapabilities.getServerPlayerPatch(player) != null
                && player.getAttribute(EpicFightAttributes.MAX_STAMINA) != null
                && player.getAttribute(EpicFightAttributes.STAMINA_REGEN) != null;
    }

    @Override
    public double current(ServerPlayer player) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        return patch == null ? 0.0D : patch.getStamina();
    }

    @Override
    public double maximum(ServerPlayer player) {
        var instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        return instance == null ? 0.0D : instance.getValue();
    }

    @Override
    public double regenMultiplier(ServerPlayer player) {
        var instance = player.getAttribute(EpicFightAttributes.STAMINA_REGEN);
        return instance == null ? 0.0D : instance.getValue();
    }

    @Override
    public void setCurrent(ServerPlayer player, double value) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.setStamina((float) value);
    }

    @Override
    public boolean actionBlocksRegen(ServerPlayer player) {
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        return patch != null && patch.getEntityState().inaction();
    }
}
