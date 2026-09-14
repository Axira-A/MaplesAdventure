package dev.maplesadventure.progression.spell;

import net.minecraft.server.level.ServerPlayer;

public interface SpellScalingAdapter {
    void refresh(ServerPlayer player);
    SpellSchoolScalingSnapshot snapshot(ServerPlayer player);
}
