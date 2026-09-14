package dev.maplesadventure.message;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.maplesadventure.MaplesAdventure;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class MessageEvents {
    public static void register() {
        NeoForge.EVENT_BUS.register(new MessageEvents());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) MessageSyncService.tick(player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MessageSyncService.syncNow(player);
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MessageSyncService.syncNow(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MessageSyncService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            MessageManager.removeSupportedAt(level, event.getPos());
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if ((event.getServer().getTickCount() % 6000) == 0) {
            int removed = MessageManager.removeExpired(event.getServer(), System.currentTimeMillis());
            if (removed > 0) MaplesAdventure.LOGGER.info("Cleaned {} expired or unsupported adventure messages", removed);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        MessageSyncService.clear();
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("maplesadventure")
                        .then(Commands.literal("message")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("messageId", StringArgumentType.word())
                                                .executes(context -> removeCommand(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "messageId")
                                                ))))
                                .then(Commands.literal("inspect")
                                        .executes(context -> inspectCommand(context.getSource().getPlayerOrException())))
                        )
        );
    }

    private static int removeCommand(net.minecraft.commands.CommandSourceStack source, String idText) {
        try {
            UUID id = UUID.fromString(idText);
            if (MessageManager.deleteAdministrative(source.getServer(), id)) {
                source.sendSuccess(() -> Component.literal("Removed message " + id), true);
                return 1;
            }
        } catch (IllegalArgumentException ignored) {
            // A malformed UUID is user command input, not a provider/runtime compatibility fault.
        }
        source.sendFailure(Component.literal("Message not found or invalid UUID"));
        return 0;
    }

    private static int inspectCommand(ServerPlayer player) {
        AdventureMessage message = MessageManager.nearest(player, 8.0D).orElse(null);
        if (message == null) {
            player.sendSystemMessage(Component.literal("No adventure message within 8 blocks"));
            return 0;
        }
        player.sendSystemMessage(Component.literal("messageId=" + message.messageId()));
        player.sendSystemMessage(Component.literal("author=" + message.authorUuid() + " (" + message.authorCachedName() + ")"));
        player.sendSystemMessage(Component.literal("dimension=" + message.dimension().location() + " position=" + message.position()));
        player.sendSystemMessage(Component.literal("phrases=" + message.phrases().stream()
                .map(phrase -> phrase.templateId() + phrase.slots() + phrase.modifiers()).toList()
                + " connectors=" + message.connectors()));
        player.sendSystemMessage(Component.literal("ratings=+" + message.positiveRatings() + "/-" + message.negativeRatings()
                + " createdAt=" + Instant.ofEpochMilli(message.createdAt())));
        return 1;
    }

    private MessageEvents() {
    }
}
