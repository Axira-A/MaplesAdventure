package dev.maplesadventure.client.editor;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Keeps toolbar layout separate from inspector and input capture. */
public final class EditorToolbar {
    public record Item(String key,Runnable action,BooleanSupplier selected,BooleanSupplier enabled) {}
    public interface Host {void item(int x,int width,Item item);}
    public static void build(int width,List<Item> items,Host host){int cell=Math.max(24,(width-10)/items.size());for(int i=0;i<items.size();i++)host.item(5+i*cell,cell-3,items.get(i));}
    private EditorToolbar(){}
}
