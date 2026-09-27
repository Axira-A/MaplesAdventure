package dev.maplesadventure.flask;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FlaskNetwork {
    public static void register(PayloadRegistrar r) {
        r.playToServer(FlaskPayloads.Request.TYPE, FlaskPayloads.Request.CODEC, (p,c) -> {
            if (c.player() instanceof ServerPlayer player) FlaskMenuService.request(player,p);
        });
        r.playToClient(FlaskPayloads.Snapshot.TYPE, FlaskPayloads.Snapshot.CODEC, (p,c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) dev.maplesadventure.client.flask.FlaskClient.receive(p);
        });
        r.playToClient(FlaskPayloads.Menu.TYPE, FlaskPayloads.Menu.CODEC, (p,c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) dev.maplesadventure.client.flask.FlaskClient.menu(p);
        });
    }
    private FlaskNetwork() {}
}
