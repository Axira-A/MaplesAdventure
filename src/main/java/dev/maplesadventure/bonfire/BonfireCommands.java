package dev.maplesadventure.bonfire;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Works from players, command blocks and functions (explicit positions; no player executor required). */
public final class BonfireCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new BonfireCommands()); }
    @SubscribeEvent public void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ma").then(root()));
        event.getDispatcher().register(Commands.literal("maplesadventure").then(root()));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root() {
        var root = Commands.literal("bonfire").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("info").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> info(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))));
        root.then(Commands.literal("name").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> name(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"),
                                StringArgumentType.getString(ctx, "name"))))));
        root.then(Commands.literal("feature").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("feature", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(BonfireFeature.values()).map(BonfireFeature::id), builder))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> feature(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"),
                                        StringArgumentType.getString(ctx, "feature"), BoolArgumentType.getBool(ctx, "enabled")))))));
        root.then(Commands.literal("resetplayer").then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        return root;
    }
    private static BonfireBlockEntity find(CommandSourceStack source, BlockPos pos) {
        if (!(source.getLevel() instanceof ServerLevel level)) return null;
        return BonfireStateService.resolve(level, pos);
    }
    private static int info(CommandSourceStack source, BlockPos pos) {
        BonfireBlockEntity entity = find(source, pos);
        if (entity == null) return missing(source);
        source.sendSuccess(() -> Component.literal("Bonfire " + entity.ref().dimension() + " " + pos.toShortString()
                + " generation=" + entity.generation() + " name=" + entity.displayName()
                + " features=" + entity.features()), false);
        return 1;
    }
    private static int name(CommandSourceStack source, BlockPos pos, String name) {
        BonfireBlockEntity entity = find(source, pos);
        if (entity == null) return missing(source);
        entity.setDisplayName(name);
        sync(source, pos);
        source.sendSuccess(() -> Component.translatable("command.maplesadventure.bonfire.named"), true);
        return 1;
    }
    private static int feature(CommandSourceStack source, BlockPos pos, String id, boolean enabled) {
        BonfireBlockEntity entity = find(source, pos);
        if (entity == null) return missing(source);
        BonfireFeature feature = BonfireFeature.byId(id).orElse(null);
        if (feature == null) {
            source.sendFailure(Component.translatable("command.maplesadventure.bonfire.invalid_feature"));
            return 0;
        }
        entity.setFeature(feature, enabled);
        sync(source, pos);
        source.sendSuccess(() -> Component.translatable("command.maplesadventure.bonfire.feature_set", feature.id(), enabled), true);
        return 1;
    }
    private static int reset(CommandSourceStack source, ServerPlayer player) {
        player.setData(dev.maplesadventure.progression.ProgressionAttachments.PLAYER_BONFIRES, new PlayerBonfireState());
        BonfireSessionService.close(player);
        BonfireStateService.sync(player);
        source.sendSuccess(() -> Component.translatable("command.maplesadventure.bonfire.reset_player", player.getName()), true);
        return 1;
    }
    private static int missing(CommandSourceStack source) {
        source.sendFailure(Component.translatable("command.maplesadventure.bonfire.missing"));
        return 0;
    }
    private static void sync(CommandSourceStack source, BlockPos pos) {
        var state = source.getLevel().getBlockState(pos);
        source.getLevel().sendBlockUpdated(pos, state, state, 3);
    }
    private BonfireCommands() {}
}
