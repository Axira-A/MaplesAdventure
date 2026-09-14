package dev.maplesadventure.progression.weapon;
import dev.maplesadventure.MaplesAdventure;
import net.neoforged.fml.ModList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
public final class WeaponIntegrations {
    public interface Affinity { Map<ResourceLocation,Double> schools(ItemStack stack); }
    public static WeaponIntegration epic;
    public static Affinity irons;
    public static void initialize() {
        if(ModList.get().isLoaded("epicfight")) epic=load("dev.maplesadventure.integration.epicfight.progression.EpicFightWeaponRequirements",WeaponIntegration.class);
        if(ModList.get().isLoaded("irons_spellbooks")) irons=load("dev.maplesadventure.integration.irons.progression.IronsWeaponAffinity",Affinity.class);
    }
    private static <T> T load(String name,Class<T> type) {
        try { return type.cast(Class.forName(name).getConstructor().newInstance()); }
        catch(ReflectiveOperationException|LinkageError error) { MaplesAdventure.LOGGER.error("Weapon adapter unavailable: {}",name,error); return null; }
    }
    private WeaponIntegrations() {}
}
