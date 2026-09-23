package dev.maplesadventure.registry;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.bonfire.BonfireBlock;
import dev.maplesadventure.bonfire.BonfireBlockEntity;
import dev.maplesadventure.multiplayer.encounter.fog.BossFogGateBlock;
import dev.maplesadventure.multiplayer.encounter.fog.BossFogGateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MaplesAdventure.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MaplesAdventure.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MaplesAdventure.MOD_ID);

    public static final DeferredBlock<BossFogGateBlock> BOSS_FOG_GATE = BLOCKS.register(
            "boss_fog_gate", BossFogGateBlock::new);
    public static final DeferredBlock<BonfireBlock> BONFIRE = BLOCKS.register("bonfire", BonfireBlock::new);
    public static final DeferredItem<BlockItem> BONFIRE_ITEM = ITEMS.register(
            "bonfire", () -> new BlockItem(BONFIRE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BonfireBlockEntity>> BONFIRE_ENTITY =
            BLOCK_ENTITIES.register("bonfire", () -> BlockEntityType.Builder.of(
                    BonfireBlockEntity::new, BONFIRE.get()).build(null));
    public static final DeferredItem<BlockItem> BOSS_FOG_GATE_ITEM = ITEMS.register(
            "boss_fog_gate", () -> new BlockItem(BOSS_FOG_GATE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BossFogGateBlockEntity>> BOSS_FOG_GATE_ENTITY =
            BLOCK_ENTITIES.register("boss_fog_gate", () -> BlockEntityType.Builder.of(
                    BossFogGateBlockEntity::new, BOSS_FOG_GATE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlocks() {}
}
