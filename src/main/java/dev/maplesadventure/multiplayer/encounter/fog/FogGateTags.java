package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.MaplesAdventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class FogGateTags {
    public static final TagKey<Block> BOSS_ROOM_BOUNDARY = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "boss_room_boundary"));
    private FogGateTags() {}
}
