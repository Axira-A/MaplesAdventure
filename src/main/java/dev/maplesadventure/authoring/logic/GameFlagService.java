package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.api.editor.logic.LogicContext;
import dev.maplesadventure.editor.EditorAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class GameFlagService {
    public enum Scope { WORLD, PLAYER }
    public static final class WorldFlags extends SavedData {
        private static final Factory<WorldFlags> FACTORY=new Factory<>(WorldFlags::new,WorldFlags::load);
        final GameFlagState flags=new GameFlagState();
        public static WorldFlags get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(FACTORY,"maplesadventure_game_flags");}
        public static WorldFlags load(CompoundTag t,HolderLookup.Provider r){var data=new WorldFlags();data.flags.deserializeNBT(r,t);return data;}
        @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){return flags.serializeNBT(r);}
    }
    public static boolean get(LogicContext ctx,Scope scope,ResourceLocation id){return state(ctx,scope).get(id);}
    public static void set(LogicContext ctx,Scope scope,ResourceLocation id,boolean value){
        if(!ctx.server().isSameThread())throw new IllegalStateException("Game flags require server thread");
        state(ctx,scope).set(id,value);if(scope==Scope.WORLD)WorldFlags.get(ctx.server()).setDirty();
    }
    private static GameFlagState state(LogicContext ctx,Scope scope){return scope==Scope.WORLD?WorldFlags.get(ctx.server()).flags:
            ctx.player().orElseThrow(()->new IllegalArgumentException("Player flag requires event player")).getData(EditorAttachments.FLAGS);}
    private GameFlagService(){}
}
