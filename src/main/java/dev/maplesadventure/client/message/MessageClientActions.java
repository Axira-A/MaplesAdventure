package dev.maplesadventure.client.message;

import dev.maplesadventure.message.MessagePhrase;
import dev.maplesadventure.message.MessageRating;
import dev.maplesadventure.network.MessagePayloads;
import java.util.UUID;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MessageClientActions {
    public static void openComposer() {
        Minecraft.getInstance().setScreen(new MessageComposerScreen());
    }

    public static void create(MessagePhrase phrase) {
        PacketDistributor.sendToServer(new MessagePayloads.Create(phrase));
    }

    public static void create(List<MessagePhrase> phrases, List<String> connectors) {
        PacketDistributor.sendToServer(new MessagePayloads.Create(phrases, connectors));
    }

    public static void requestRead(UUID messageId) {
        PacketDistributor.sendToServer(new MessagePayloads.Read(messageId));
    }

    public static void rate(UUID messageId, MessageRating rating) {
        PacketDistributor.sendToServer(new MessagePayloads.Rate(messageId, rating));
    }

    public static void delete(UUID messageId) {
        PacketDistributor.sendToServer(new MessagePayloads.Delete(messageId));
    }

    private MessageClientActions() {
    }
}
