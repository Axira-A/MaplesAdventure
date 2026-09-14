package dev.maplesadventure.progression.spell;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.PlayerAttributeService;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** No Iron's classes in the core. No cached equipment values, no per-tick refresh. */
public final class SpellScalingRuntimeService {
    private static SpellScalingAdapter adapter;
    private static boolean resolved;
    private static SpellScalingAdapter adapter() {
        if (!resolved) {
            resolved = true;
            if (ModList.get().isLoaded("irons_spellbooks")) try {
                adapter = (SpellScalingAdapter) Class.forName(
                        "dev.maplesadventure.integration.irons.progression.IronsSpellScalingAdapter").getConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError failure) {
                MaplesAdventure.LOGGER.error("Iron's school scaling adapter unavailable", failure);
            }
        }
        return adapter;
    }
    public static boolean available() { return adapter() != null; }
    public static void refresh(ServerPlayer player) { if (adapter() != null) adapter.refresh(player); }
    public static SpellSchoolScalingSnapshot snapshot(ServerPlayer player) {
        if (adapter() != null) return adapter.snapshot(player);
        Map<net.minecraft.resources.ResourceLocation, SpellSchoolStat> result = new LinkedHashMap<>();
        var state = PlayerAttributeService.state(player);
        SpellSchoolScalingRegistry.profiles().forEach((id, profile) -> result.put(id,
                new SpellSchoolStat(profile, Component.translatable("school." + id.getNamespace() + "." + id.getPath()),
                        profile.bonus(state), SpellPowerContext.unavailable(), false)));
        return new SpellSchoolScalingSnapshot(result);
    }
    private SpellScalingRuntimeService() {}
}
