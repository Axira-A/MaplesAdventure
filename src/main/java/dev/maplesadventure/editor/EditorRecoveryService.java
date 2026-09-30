package dev.maplesadventure.editor;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.authoring.EditorTransform;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Server-owned transition. The marker and vanilla game mode persist in the same player NBT. */
public final class EditorRecoveryService {
    public static void begin(ServerPlayer player){
        var state=player.getData(EditorAttachments.RECOVERY);
        if(state.active())restore(player,false);
        // A third-party veto may leave recovery pending. Never overwrite the original return
        // point with the still-active spectator state on another OPEN request.
        if(state.active())throw new IllegalStateException("editor.maplesadventure.spectator_refused");
        state.begin(new EditorRecoveryState.ReturnPoint(player.gameMode.getGameModeForPlayer(),player.level().dimension().location(),
                new EditorTransform(player.position(),net.minecraft.util.Mth.wrapDegrees(player.getYRot()),player.getXRot())));
        // Install before changing mode: any subsequent player save contains both values. A crash
        // before that save leaves the previous non-editor player record, not a stranded spectator.
        try{
            player.setCamera(player);
            if(!player.setGameMode(GameType.SPECTATOR)||!player.isSpectator()){
                // Already-spectator owners are valid too; a cancelled external game mode event is not.
                if(!player.isSpectator())throw new IllegalStateException("editor.maplesadventure.spectator_refused");
            }
        }catch(RuntimeException failure){restore(player,false);throw failure;}
    }
    /** External game-mode commands keep their explicit choice, but still return the author to origin. */
    public static void restore(ServerPlayer player,boolean preserveExternalMode){
        if(!player.hasData(EditorAttachments.RECOVERY)||!player.getData(EditorAttachments.RECOVERY).active()||!player.isAlive())return;
        var state=player.getData(EditorAttachments.RECOVERY);var point=state.point().orElse(null);
        var level=point==null?null:player.server.getLevel(ResourceKey.create(Registries.DIMENSION,point.dimension()));
        var transform=point==null?null:point.transform();
        if(level==null){
            level=player.server.overworld();var spawn=level.getSharedSpawnPos();
            transform=new EditorTransform(new Vec3(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5),level.getSharedSpawnAngle(),0);
            MaplesAdventure.LOGGER.warn("Editor return fallback player={} reason={}",player.getUUID(),state.malformed()?"malformed recovery":"missing dimension");
        }
        player.setCamera(player);
        var pos=transform.position();
        player.teleportTo(level,pos.x,pos.y,pos.z,transform.yaw(),transform.pitch());
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        if(!preserveExternalMode){
            var expected=point==null?GameType.SURVIVAL:point.gameMode();
            player.setGameMode(expected);
            if(player.gameMode.getGameModeForPlayer()!=expected){
                MaplesAdventure.LOGGER.warn("Editor recovery game-mode change refused for {}; retaining recovery marker",player.getUUID());
                return;
            }
        }
        state.clear();
    }
    private EditorRecoveryService(){}
}
