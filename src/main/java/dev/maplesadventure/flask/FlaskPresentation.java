package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.FlaskKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Placeholder sound replacement seam. Vanilla DRINK supplies animation; no source-less particle broadcast. */
public final class FlaskPresentation {
    public static void effect(ServerPlayer player, FlaskKind kind) {
        player.level().playSound(null, player, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.0F);
    }
    private FlaskPresentation() {}
}
