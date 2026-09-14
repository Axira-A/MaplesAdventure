package dev.maplesadventure.integration.epicfight.progression;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import yesman.epicfight.registry.EpicFightRegistries;

/** Audited against NightFall 3.4.0. No Nightfall class is loaded by this adapter. */
public final class NightfallDodgeAdapter {
    public static final String MOD_ID = "efn";
    public static final ResourceLocation ROLL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "efn_dodge");
    public static final ResourceLocation STEP = ResourceLocation.fromNamespaceAndPath(MOD_ID, "efn_step");

    public static boolean available() {
        return ModList.get().isLoaded(MOD_ID) && EpicFightRegistries.SKILL.containsKey(ROLL)
                && EpicFightRegistries.SKILL.containsKey(STEP);
    }

    private NightfallDodgeAdapter() {}
}
