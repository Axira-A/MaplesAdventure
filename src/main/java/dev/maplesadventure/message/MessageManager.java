package dev.maplesadventure.message;

import dev.maplesadventure.config.MessageConfig;
import dev.maplesadventure.network.MessagePayloads;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MessageManager {
    private static final long MILLIS_PER_DAY = Duration.ofDays(1).toMillis();

    public static void create(ServerPlayer player, List<MessagePhrase> phrases, List<String> connectors) {
        dev.maplesadventure.multiplayer.phase.PhaseRole role =
                dev.maplesadventure.multiplayer.phase.PhaseManager.state(player).role();
        if (role == dev.maplesadventure.multiplayer.phase.PhaseRole.INVADER
                || role == dev.maplesadventure.multiplayer.phase.PhaseRole.COOPERATOR) {
            reject(player, MessageValidator.Failure.NOT_ALLOWED);
            return;
        }
        if (phrases.isEmpty() || phrases.size() > 2 || connectors.size() != phrases.size() - 1
                || phrases.stream().anyMatch(phrase -> !MessageTemplateRegistry.isValid(phrase))
                || connectors.stream().anyMatch(id -> MessageConnectorRegistry.find(id).isEmpty())) {
            reject(player, MessageValidator.Failure.INVALID_TEMPLATE);
            return;
        }
        MessageValidator.PlacementResult placement = MessageValidator.resolvePlacement(player);
        if (!placement.valid()) {
            reject(player, placement.failure());
            return;
        }

        long now = System.currentTimeMillis();
        MessageSavedData data = MessageSavedData.get(player.serverLevel().getServer());
        List<AdventureMessage> authored = data.byAuthor(player.getUUID());
        long lastCreation = data.lastCreationTime(player.getUUID());
        if (lastCreation > 0L) {
            long cooldown = MessageConfig.CREATION_COOLDOWN_SECONDS.get() * 1000L;
            if (now - lastCreation < cooldown) {
                player.displayClientMessage(Component.translatable(
                        "message.maplesadventure.create.cooldown",
                        Math.max(1L, (cooldown - (now - lastCreation) + 999L) / 1000L)
                ), true);
                return;
            }
        }

        double spacing = MessageConfig.MIN_SPACING.get();
        if (!data.nearby(player.level().dimension(), placement.position(), spacing, 1).isEmpty()) {
            reject(player, MessageValidator.Failure.TOO_CLOSE_TO_ANOTHER);
            return;
        }

        while (authored.size() >= MessageConfig.MAX_ACTIVE_PER_PLAYER.get()) {
            AdventureMessage oldest = authored.remove(0);
            removeInternal(player.serverLevel().getServer().getLevel(oldest.dimension()), data, oldest);
            player.displayClientMessage(Component.translatable("message.maplesadventure.create.oldest_removed"), true);
        }

        AdventureMessage message = new AdventureMessage(
                UUID.randomUUID(),
                player.getUUID(),
                player.getGameProfile().getName(),
                player.level().dimension(),
                placement.position(),
                placement.supportPos(),
                player.getYRot(),
                placement.hitResult().getDirection(),
                phrases,
                connectors,
                now
        );
        data.add(message);
        data.recordCreation(player.getUUID(), now);
        MessageSyncService.broadcastUpsert(player.serverLevel(), message);
        player.displayClientMessage(Component.translatable("message.maplesadventure.create.success"), true);
    }

    public static void create(ServerPlayer player, MessagePhrase phrase) {
        create(player, List.of(phrase), List.of());
    }

    public static void openReader(ServerPlayer player, UUID messageId) {
        AdventureMessage message = MessageSavedData.get(player.serverLevel().getServer()).get(messageId).orElse(null);
        if (message == null) {
            reject(player, MessageValidator.Failure.NOT_FOUND);
            return;
        }
        MessageValidator.ReadResult validation = MessageValidator.canRead(player, message);
        if (!validation.valid()) {
            reject(player, validation.failure());
            return;
        }
        PacketDistributor.sendToPlayer(player, new MessagePayloads.OpenReader(
                message.summary(), message.ratingBy(player.getUUID()), message.authorUuid().equals(player.getUUID())
        ));
    }

    public static void rate(ServerPlayer player, UUID messageId, MessageRating requested) {
        MessageSavedData data = MessageSavedData.get(player.serverLevel().getServer());
        AdventureMessage message = data.get(messageId).orElse(null);
        if (message == null || requested == MessageRating.NONE || message.authorUuid().equals(player.getUUID())) {
            reject(player, MessageValidator.Failure.NOT_ALLOWED);
            return;
        }
        MessageValidator.ReadResult validation = MessageValidator.canRead(player, message);
        if (!validation.valid()) {
            reject(player, validation.failure());
            return;
        }
        if (message.updateRating(player.getUUID(), requested)) {
            data.ratingChanged();
            MessageSyncService.broadcastUpsert(player.serverLevel(), message);
        }
        PacketDistributor.sendToPlayer(player, new MessagePayloads.OpenReader(
                message.summary(), message.ratingBy(player.getUUID()), false
        ));
    }

    public static boolean delete(ServerPlayer player, UUID messageId) {
        MessageSavedData data = MessageSavedData.get(player.serverLevel().getServer());
        AdventureMessage message = data.get(messageId).orElse(null);
        if (message == null) return false;
        if (!message.authorUuid().equals(player.getUUID()) && !player.hasPermissions(2)) {
            reject(player, MessageValidator.Failure.NOT_ALLOWED);
            return false;
        }
        removeInternal(player.serverLevel().getServer().getLevel(message.dimension()), data, message);
        player.displayClientMessage(Component.translatable("message.maplesadventure.delete.success"), true);
        return true;
    }

    public static boolean deleteAdministrative(net.minecraft.server.MinecraftServer server, UUID messageId) {
        MessageSavedData data = MessageSavedData.get(server);
        AdventureMessage message = data.get(messageId).orElse(null);
        if (message == null) return false;
        removeInternal(server.getLevel(message.dimension()), data, message);
        return true;
    }

    public static int removeExpired(net.minecraft.server.MinecraftServer server, long now) {
        MessageSavedData data = MessageSavedData.get(server);
        long maximumAge = MessageConfig.LIFETIME_DAYS.get() * MILLIS_PER_DAY;
        int removed = 0;
        for (AdventureMessage message : data.allMessages()) {
            ServerLevel level = server.getLevel(message.dimension());
            boolean expired = now - message.createdAt() >= maximumAge;
            boolean unsupported = level != null && level.isLoaded(message.supportPos())
                    && !MessageValidator.supportIsValid(level, message);
            if (expired || unsupported) {
                removeInternal(level, data, message);
                removed++;
            }
        }
        return removed;
    }

    public static Optional<AdventureMessage> nearest(ServerPlayer player, double radius) {
        return MessageSavedData.get(player.serverLevel().getServer())
                .nearby(player.level().dimension(), player.position(), radius, 1).stream().findFirst();
    }

    public static int removeSupportedAt(ServerLevel level, net.minecraft.core.BlockPos supportPos) {
        MessageSavedData data = MessageSavedData.get(level.getServer());
        int removed = 0;
        for (AdventureMessage message : data.nearby(level.dimension(), net.minecraft.world.phys.Vec3.atCenterOf(supportPos), 2.0D, 64)) {
            if (message.supportPos().equals(supportPos)) {
                removeInternal(level, data, message);
                removed++;
            }
        }
        return removed;
    }

    private static void removeInternal(ServerLevel level, MessageSavedData data, AdventureMessage message) {
        data.remove(message.messageId());
        if (level != null) MessageSyncService.broadcastRemove(level, message);
    }

    private static void reject(ServerPlayer player, MessageValidator.Failure failure) {
        player.displayClientMessage(Component.translatable(
                "message.maplesadventure.failure." + failure.name().toLowerCase(java.util.Locale.ROOT)
        ), true);
    }

    private MessageManager() {
    }
}
