package dev.maplesadventure.api.editor.client;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Register exclusively from client setup. Common component descriptors must never reference this class. */
public final class MaplesEditorClientApi {
    private static final Map<ResourceLocation,EditorGizmoProvider> PROVIDERS=new HashMap<>();
    private static boolean frozen;
    public static synchronized void registerGizmo(ResourceLocation type,EditorGizmoProvider provider){
        if(frozen)throw new IllegalStateException("Editor gizmo registry frozen");
        if(PROVIDERS.putIfAbsent(Objects.requireNonNull(type),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate editor gizmo "+type);
    }
    /** Called after mod setup; no runtime provider replacement. */
    public static synchronized void freeze(){frozen=true;}
    public static Optional<EditorGizmoProvider> gizmo(ResourceLocation type){return Optional.ofNullable(PROVIDERS.get(type));}
    private MaplesEditorClientApi(){}
}
