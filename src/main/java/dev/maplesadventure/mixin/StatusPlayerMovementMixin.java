package dev.maplesadventure.mixin;

import dev.maplesadventure.progression.status.StatusControlLockService;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client input suppression is not authority. Reject movement after vanilla main-thread dispatch. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class StatusPlayerMovementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleMovePlayer",at=@At(value="FIELD",target="Lnet/minecraft/server/level/ServerPlayer;wonGame:Z"),cancellable=true)
    private void maplesadventure$lockedMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
        if(!StatusControlLockService.locked(player)) return;
        if(packet.hasPosition()&&player.position().distanceToSqr(packet.getX(player.getX()),packet.getY(player.getY()),packet.getZ(player.getZ()))>0.0001)
            player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
        ci.cancel();
    }
}
