package dev.maplesadventure.editor;

import dev.maplesadventure.MaplesAdventure;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class EditorPermissions {
    private static final Map<ResourceLocation,Predicate<ServerPlayer>> GRANTS = new LinkedHashMap<>();
    private static final Set<ResourceLocation> REPORTED = new HashSet<>();
    private static boolean frozen;
    public static synchronized void register(ResourceLocation id,Predicate<ServerPlayer> provider) {
        if(frozen)throw new IllegalStateException("Editor permissions frozen");
        if(GRANTS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate permission provider");
    }
    public static void freeze(){frozen=true;}
    public static boolean authorized(ServerPlayer player) {
        if(player.hasPermissions(2)||player.server.isSingleplayerOwner(player.getGameProfile()))return true;
        for(var entry:GRANTS.entrySet())try{if(entry.getValue().test(player))return true;}
        catch(RuntimeException|LinkageError error){if(REPORTED.add(entry.getKey()))MaplesAdventure.LOGGER.warn("Editor permission provider {} failed",entry.getKey(),error);}
        return false;
    }
    private EditorPermissions(){}
}
