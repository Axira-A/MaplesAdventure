package dev.maplesadventure.editor.network;

import dev.maplesadventure.editor.EditorSessionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EditorNetwork {
    public static void register(PayloadRegistrar r){
        r.playToServer(EditorPayloads.Request.TYPE,EditorPayloads.Request.CODEC,(p,c)->{
            if(c.player() instanceof ServerPlayer player)EditorSessionService.request(player,p);
        });
        r.playToClient(EditorPayloads.Page.TYPE,EditorPayloads.Page.CODEC,(p,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)dev.maplesadventure.client.editor.EditorClient.receive(p);
        });
    }
    private EditorNetwork(){}
}
