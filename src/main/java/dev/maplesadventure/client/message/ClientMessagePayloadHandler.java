package dev.maplesadventure.client.message;

import dev.maplesadventure.interaction.InteractionTargetManager;
import dev.maplesadventure.network.MessagePayloads;
import net.minecraft.client.Minecraft;

public final class ClientMessagePayloadHandler {
    public static void handle(MessagePayloads.NearbySnapshot payload) {
        MessageRenderCache.replace(payload.messages());
        InteractionTargetManager.getInstance().refreshCandidatesNow();
    }

    public static void handle(MessagePayloads.Upsert payload) {
        MessageRenderCache.upsert(payload.message());
    }

    public static void handle(MessagePayloads.Remove payload) {
        MessageRenderCache.remove(payload.messageId());
        InteractionTargetManager.getInstance().refreshCandidatesNow();
        if (Minecraft.getInstance().screen instanceof MessageReaderScreen reader
                && reader.messageId().equals(payload.messageId())) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public static void handle(MessagePayloads.OpenReader payload) {
        MessageRenderCache.upsert(payload.message());
        Minecraft.getInstance().setScreen(new MessageReaderScreen(
                payload.message(), payload.viewerRating(), payload.ownMessage()
        ));
    }

    private ClientMessagePayloadHandler() {
    }
}
