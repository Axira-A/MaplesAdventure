package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.PlayerAttributeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class VanillaHealthAdapter implements DerivedStatRuntimeAdapter {
    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MaplesAdventure.MOD_ID, "progression_vigor_health");

    @Override public DerivedRuntimeResource resource() { return DerivedRuntimeResource.HEALTH; }

    @Override
    public RuntimeResourceValue refresh(ServerPlayer player, PlayerAttributeState state, DerivedStatRefreshReason reason) {
        var instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) throw new IllegalStateException("Player has no MAX_HEALTH attribute");
        double formula = resource().formula(state);
        return AttributeRuntimeSupport.apply(instance, MODIFIER_ID, formula, resource().progressionBaseline(),
                player::getHealth, value -> player.setHealth((float) value));
    }

    @Override public RuntimeResourceValue inspect(ServerPlayer player, PlayerAttributeState state) {
        var instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) throw new IllegalStateException("Player has no MAX_HEALTH attribute");
        return AttributeRuntimeSupport.inspect(instance, resource().formula(state));
    }
}
