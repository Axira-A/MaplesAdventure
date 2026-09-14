package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.MaplesAdventure;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProgressionDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MaplesAdventure.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponInfusionState>> WEAPON_INFUSION =
            COMPONENTS.registerComponentType("weapon_infusion", builder -> builder.persistent(WeaponInfusionState.CODEC)
                    .networkSynchronized(WeaponInfusionState.STREAM_CODEC));
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
    private ProgressionDataComponents() {}
}
