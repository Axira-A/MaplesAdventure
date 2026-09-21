package dev.maplesadventure.api.defense;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dev.maplesadventure.api.damage.MaplesDamageChannel;
import dev.maplesadventure.api.status.*;
import dev.maplesadventure.integration.api.StatusApiBridge;
import dev.maplesadventure.integration.api.ApiNotifications;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Immutable queries and validated non-player profile assignment over existing authoritative services. */
public final class MaplesDefenseApi {
    private MaplesDefenseApi() {}
    /**
     * @param target living server entity
     * @return empty for invalid/client/off-thread calls */
    public static Optional<DefenseView> query(LivingEntity target) {
        if (StatusApiBridge.invalid(target) != null) return Optional.empty();
        var resolved = TargetDefenseResolver.resolve(target);
        var channels = new EnumMap<MaplesDamageChannel, DefenseView.Channel>(MaplesDamageChannel.class);
        for (var c : MaplesDamageChannel.values()) {
            var d = resolved.view().channel(WeaponDamageChannel.valueOf(c.name()));
            channels.put(c, new DefenseView.Channel(d.defense(), d.absorption()));
        }
        var statuses = new EnumMap<MaplesStatusType,StatusView>(MaplesStatusType.class);
        for (var s : MaplesStatusType.values()) statuses.put(s, StatusApiBridge.snapshot(target,s));
        var profile = EntityDefenseService.resolve(target);
        return Optional.of(new DefenseView(channels,statuses,target instanceof Player ? Optional.empty() :
                Optional.of(profile.requestedId()),profile.unknown(),resolved.pressure()));
    }
    /**
     * @param target server entity
     * @param channel channel
     * @return channel defense or empty for invalid context */
    public static Optional<DefenseView.Channel> channel(LivingEntity target, MaplesDamageChannel channel) {
        return channel == null ? Optional.empty() : query(target).map(v -> v.channels().get(channel));
    }
    /**
     * @param target server entity
     * @param type ailment
     * @return effective resistance/status view */
    public static Optional<StatusView> status(LivingEntity target, MaplesStatusType type) {
        return MaplesStatusApi.query(target,type);
    }
    /**
     * Assigns an existing datapack profile using the established persistent reference.
     *
     * @param target non-player server entity
     * @param profile existing profile ID
     *
     * @return false for invalid/unknown/player/client/off-thread/reentrant requests; no mutation then
     */
    public static boolean assignProfile(LivingEntity target, ResourceLocation profile) {
        if (profile == null || StatusApiBridge.invalid(target) != null || target instanceof Player || ApiNotifications.busy()) return false;
        try { EntityDefenseService.assign(target,profile); return true; }
        catch (IllegalArgumentException invalid) { return false; }
    }
    /**
     * @param target non-player server entity
     * @return false for invalid contexts; otherwise restores datapack rule resolution */
    public static boolean clearProfileOverride(LivingEntity target) {
        if (StatusApiBridge.invalid(target) != null || target instanceof Player || ApiNotifications.busy()) return false;
        EntityDefenseService.clearOverride(target); return true;
    }
}
