package dev.maplesadventure.multiplayer.echo.client;

import dev.maplesadventure.multiplayer.echo.EchoAppearanceSnapshot;
import dev.maplesadventure.multiplayer.echo.EchoFrame;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class EchoPlayback {
    private final UUID playbackId;
    private final ResourceLocation dimension;
    private final EchoAppearanceSnapshot appearance;
    private final List<EchoFrame> frames;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private float age;

    EchoPlayback(UUID playbackId, ResourceLocation dimension, EchoAppearanceSnapshot appearance,
                 List<EchoFrame> frames, int fadeInTicks, int fadeOutTicks) {
        this.playbackId = playbackId;
        this.dimension = dimension;
        this.appearance = appearance;
        this.frames = List.copyOf(frames);
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    void tick() { age += 1.0F; }
    boolean finished() { return age > playbackDuration() + fadeOutTicks; }
    UUID playbackId() { return playbackId; }
    ResourceLocation dimension() { return dimension; }
    EchoAppearanceSnapshot appearance() { return appearance; }

    float alpha(float partialTick) {
        float current = age + partialTick;
        float fadeIn = fadeInTicks <= 0 ? 1.0F : Mth.clamp(current / fadeInTicks, 0.0F, 1.0F);
        float fadeOut = fadeOutTicks <= 0 ? 1.0F
                : Mth.clamp((playbackDuration() + fadeOutTicks - current) / fadeOutTicks, 0.0F, 1.0F);
        return Math.min(fadeIn, fadeOut);
    }

    EchoFrame frame(float partialTick) {
        float targetTick = frames.getFirst().serverTick() + Math.min(age + partialTick, playbackDuration());
        for (int index = 1; index < frames.size(); index++) {
            EchoFrame next = frames.get(index);
            if (next.serverTick() >= targetTick) {
                EchoFrame previous = frames.get(index - 1);
                float span = Math.max(1.0F, next.serverTick() - previous.serverTick());
                return EchoFrame.interpolate(previous, next, (targetTick - previous.serverTick()) / span);
            }
        }
        return frames.getLast();
    }

    private float playbackDuration() { return Math.max(1, frames.getLast().serverTick() - frames.getFirst().serverTick()); }
}
