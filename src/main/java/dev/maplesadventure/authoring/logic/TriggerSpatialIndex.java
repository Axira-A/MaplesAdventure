package dev.maplesadventure.authoring.logic;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Chunk grid plus larger region levels; each volume occupies at most 64 cells. No chunk access. */
public final class TriggerSpatialIndex<T> {
    private static final int[] SIZES={16,64,256,1024,4096};
    private record Cell(ResourceLocation dimension,int size,int x,int z){}
    private final Map<Cell,LinkedHashSet<T>> cells=new HashMap<>();
    public void add(ResourceLocation dimension,TriggerVolume v,T value){
        var p=v.transform().position();double r=v.coverage();
        for(int size:SIZES){int x0=cell(p.x-r,size),x1=cell(p.x+r,size),z0=cell(p.z-r,size),z1=cell(p.z+r,size);
            if((long)(x1-x0+1)*(z1-z0+1)>64)continue;
            for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)cells.computeIfAbsent(new Cell(dimension,size,x,z),k->new LinkedHashSet<>()).add(value);return;
        }throw new IllegalArgumentException("Trigger coverage exceeds spatial index limit");
    }
    public Set<T> candidates(ResourceLocation dimension,Vec3 p){var result=new LinkedHashSet<T>();
        for(int size:SIZES){var entries=cells.get(new Cell(dimension,size,cell(p.x,size),cell(p.z,size)));if(entries!=null)result.addAll(entries);}return result;}
    private static int cell(double v,int size){return (int)Math.floor(v/size);}
    public void clear(){cells.clear();}
}
