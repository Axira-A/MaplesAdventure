package dev.maplesadventure.api.defense;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.*;

/**
 * Detached immutable effective defense snapshot. Player views respect the server enable setting.
 *
     * @param channels nine damage channels
 *
     * @param statuses seven effective status views
 *
     * @param profile non-player requested profile ID; empty for players
 *
     * @param unknownProfile whether a saved reference no longer resolves
 *
     * @param pressure current server mitigation-pressure configuration
 */
public record DefenseView(Map<MaplesDamageChannel, Channel> channels, Map<MaplesStatusType, StatusView> statuses,
        Optional<ResourceLocation> profile, boolean unknownProfile, double pressure) {
    /** Immutable biological/material defense, independent of vanilla armor and absorption hearts.
     *
     * @param defense defense value
     * @param absorption fractional absorption, not absorption hearts */
    public record Channel(double defense, double absorption) {}
    /** Defensively copies maps. */
    public DefenseView {
        channels = Map.copyOf(channels); statuses = Map.copyOf(statuses);
        java.util.Objects.requireNonNull(profile);
    }
}
