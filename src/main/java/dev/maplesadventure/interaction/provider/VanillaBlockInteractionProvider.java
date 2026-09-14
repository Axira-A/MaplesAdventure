package dev.maplesadventure.interaction.provider;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.interaction.BlockInteractionTarget;
import dev.maplesadventure.interaction.InteractionPriority;
import dev.maplesadventure.interaction.InteractionTarget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class VanillaBlockInteractionProvider implements InteractionTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "vanilla_block");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean supports(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof BlockInteractionTarget blockTarget) || !level.isLoaded(blockTarget.pos())) {
            return false;
        }
        BlockState state = level.getBlockState(blockTarget.pos());
        if (state.getBlock() != blockTarget.expectedBlock() || state.isAir()) {
            return false;
        }
        MenuProvider menu = state.getMenuProvider(level, blockTarget.pos());
        return menu != null || isKnownInteractiveBlock(state);
    }

    @Override
    public boolean isValidTarget(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        if (!(target instanceof BlockInteractionTarget blockTarget) || !level.isLoaded(blockTarget.pos())) {
            return false;
        }
        BlockState state = level.getBlockState(blockTarget.pos());
        return state.getBlock() == blockTarget.expectedBlock() && !state.isAir();
    }

    @Override
    public Component getDisplayName(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        BlockInteractionTarget blockTarget = (BlockInteractionTarget) target;
        BlockState state = level.getBlockState(blockTarget.pos());
        Item item = state.getBlock().asItem();
        if (item != net.minecraft.world.item.Items.AIR) {
            ItemStack stack = item.getDefaultInstance();
            return stack.getHoverName();
        }
        return state.getBlock().getName();
    }

    @Override
    public InteractionPriority getPriority(ClientLevel level, LocalPlayer player, InteractionTarget target) {
        BlockInteractionTarget blockTarget = (BlockInteractionTarget) target;
        return level.getBlockState(blockTarget.pos()).getMenuProvider(level, blockTarget.pos()) != null
                ? InteractionPriority.HIGH
                : InteractionPriority.NORMAL;
    }

    private static boolean isKnownInteractiveBlock(BlockState state) {
        Object block = state.getBlock();
        return block instanceof ChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof CraftingTableBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof AnvilBlock
                || block instanceof EnchantingTableBlock
                || block instanceof BrewingStandBlock
                || block instanceof BeaconBlock
                || block instanceof BedBlock
                || block instanceof BellBlock
                || block instanceof LecternBlock
                || block instanceof GrindstoneBlock
                || block instanceof StonecutterBlock
                || block instanceof SmithingTableBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof ComposterBlock
                || block instanceof CakeBlock;
    }
}
