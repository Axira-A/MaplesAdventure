package dev.maplesadventure.regression;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.flask.*;
import dev.maplesadventure.flask.*;
import dev.maplesadventure.progression.ProgressionAttachments;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Real ServerPlayer lethal damage + Clone/Respawn events, isolated fixture UUIDs only. */
public final class FlaskDeathRegression {
    private static UUID dying;
    private static List<ItemStack> drops = List.of();
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDropsEvent.class, e -> {
            if (!e.getEntity().getUUID().equals(dying)) return;
            drops = e.getDrops().stream().map(d -> d.getItem().copy()).toList();
            e.setCanceled(true); // Capture without leaving test loot in the isolated world.
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("flaskdeathregression").requires(s -> s.hasPermission(2)).executes(c -> run(c.getSource()))));
    }
    private static void check(boolean value, String label) {
        if (!value) throw new IllegalStateException("Flask death regression: " + label);
        MaplesAdventure.LOGGER.info("[Flask death regression] PASS {}", label);
    }
    private static ServerPlayer player(CommandSourceStack s, UUID id) {
        var profile = new GameProfile(id, "FlaskDeathProbe");
        var p = new ServerPlayer(s.getServer(), s.getLevel(), profile, ClientInformation.createDefault());
        p.connection = FakePlayerFactory.get(s.getLevel(), profile).connection;
        p.setPos(s.getLevel().getSharedSpawnPos().getCenter());
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); p.setHealth(5);
        return p;
    }
    private static int run(CommandSourceStack s) {
        boolean old = s.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        try {
            for (boolean keep : new boolean[]{false, true}) {
                s.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(keep, s.getServer());
                dying = UUID.randomUUID(); drops = List.of(); var p = player(s, dying);
                p.setData(ProgressionAttachments.FLASK, new FlaskState(new FlaskSnapshot(8, 5, 8, 8, 0, 0)));
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FlaskItems.CRIMSON.get()));
                p.getInventory().add(new ItemStack(FlaskItems.ASHEN.get()));
                p.getInventory().add(new ItemStack(FlaskItems.SHARD.get(), 3));
                check(FlaskUseController.start(p, FlaskKind.CRIMSON, InteractionHand.MAIN_HAND), "Start drink before lethal hit");
                for (int i = 0; i < 15; i++) FlaskUseController.tick(s.getServer());
                var before = FlaskService.state(p);
                p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
                check(p.isDeadOrDying(), "Actual lethal damage, keepInventory=" + keep);
                var clone = player(s, dying);
                NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, p, true));
                check(FlaskService.state(clone).equals(before), "Clone preserves permanent state and spent charge, no refill");
                for (int i = 0; i < 8; i++) FlaskUseController.tick(s.getServer());
                check(clone.getHealth() == 5 && !FlaskApi.isUsingFlask(clone), "Death pending recovery cannot reach replacement player");
                if (keep) clone.getInventory().replaceWith(p.getInventory());
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(clone, false));
                int ground = drops.stream().filter(i -> i.is(FlaskItems.CRIMSON.get()) || i.is(FlaskItems.ASHEN.get())).mapToInt(ItemStack::getCount).sum();
                MaplesAdventure.LOGGER.info("[Flask death regression] keepInventory={} droppedHandles={} respawnCrimson={}", keep, ground, clone.getInventory().countItem(FlaskItems.CRIMSON.get()));
                check(ground == 0, "No obsolete death handles alongside missing-handle regrant");
                check(clone.getInventory().countItem(FlaskItems.CRIMSON.get()) == 1, "Exactly one crimson handle after respawn");
                if (!keep) check(drops.stream().anyMatch(i -> i.is(FlaskItems.SHARD.get()) && i.getCount() == 3), "Materials remain normal death loot");
            }
            s.sendSuccess(() -> Component.literal("Flask death regression PASS"), false); return 1;
        } finally {
            dying = null; drops = List.of();
            s.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(old, s.getServer());
        }
    }
    private FlaskDeathRegression() {}
}
