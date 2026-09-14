package dev.maplesadventure.registry;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.soul.LostSoulEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MaplesAdventure.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<LostSoulEntity>> LOST_SOUL = ENTITY_TYPES.register(
            "lost_soul",
            () -> EntityType.Builder.<LostSoulEntity>of(LostSoulEntity::new, MobCategory.MISC)
                    .sized(0.72F, 1.55F)
                    .eyeHeight(1.05F)
                    .nameTagOffset(1.35F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build(MaplesAdventure.MOD_ID + ":lost_soul")
    );

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    private ModEntityTypes() {
    }
}
