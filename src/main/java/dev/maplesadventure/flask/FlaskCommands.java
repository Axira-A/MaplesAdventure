package dev.maplesadventure.flask;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.maplesadventure.api.flask.FlaskRechargeReason;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class FlaskCommands {
    public static void register() { NeoForge.EVENT_BUS.addListener(FlaskCommands::commands); }
    private static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("ma").then(root()));
        e.getDispatcher().register(Commands.literal("maplesadventure").then(root()));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> root() {
        var root=Commands.literal("flask").requires(s->s.hasPermission(2));
        root.then(Commands.literal("status").executes(c->status(c.getSource(),c.getSource().getPlayerOrException()))
                .then(Commands.argument("player",EntityArgument.player()).executes(c->status(c.getSource(),EntityArgument.getPlayer(c,"player")))));
        root.then(Commands.literal("refill").executes(c->{ FlaskService.refill(c.getSource().getPlayerOrException(),FlaskRechargeReason.SCRIPTED);return 1; }));
        root.then(Commands.literal("reset").executes(c->{ FlaskService.debugReset(c.getSource().getPlayerOrException());return 1; }));
        root.then(Commands.literal("set_capacity").then(Commands.argument("value",IntegerArgumentType.integer(4,14)).executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException(); FlaskService.debugSet(p,IntegerArgumentType.getInteger(c,"value"),FlaskService.state(p).potencyLevel()); return 1; })));
        root.then(Commands.literal("set_potency").then(Commands.argument("value",IntegerArgumentType.integer(0,12)).executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException(); FlaskService.debugSet(p,FlaskService.state(p).totalCapacity(),IntegerArgumentType.getInteger(c,"value")); return 1; })));
        root.then(Commands.literal("set_allocation").then(Commands.argument("crimson",IntegerArgumentType.integer(0,14))
                .then(Commands.argument("ashen",IntegerArgumentType.integer(0,14)).executes(c->{
                    boolean ok=FlaskService.allocate(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"crimson"),IntegerArgumentType.getInteger(c,"ashen"));
                    if(!ok)c.getSource().sendFailure(Component.translatable("message.maplesadventure.flask.invalid_allocation"));return ok?1:0;
                }))));
        return root;
    }
    private static int status(CommandSourceStack s,ServerPlayer p) { s.sendSuccess(()->Component.literal(FlaskService.state(p).toString()+" using="+FlaskUseController.kindId(p)),false);return 1; }
    private FlaskCommands() {}
}
