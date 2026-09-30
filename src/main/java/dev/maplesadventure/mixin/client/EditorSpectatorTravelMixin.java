package dev.maplesadventure.mixin.client;

import dev.maplesadventure.client.editor.EditorClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-only; affects only the local spectator with a server-authorized editor session. */
@Mixin(Player.class)
abstract class EditorSpectatorTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$editorTravel(Vec3 input, CallbackInfo ci) {
        if (EditorClient.travel((Player) (Object) this)) ci.cancel();
    }
}
