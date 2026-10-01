package dev.maplesadventure.api.editor.client;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Register exclusively from client setup. Common component descriptors must never reference this class. */
public final class MaplesEditorClientApi {
    private static final Map<ResourceLocation,EditorGizmoProvider> PROVIDERS=new HashMap<>();
    private static boolean frozen;
    private record Key(EditorPresentation.Target target,ResourceLocation id) {}
    private static final Map<Key,EditorPresentation> PRESENTATIONS=new HashMap<>();
    /** Additive to existing descriptors. Missing presentation uses the synchronized schema. */
    public static synchronized void registerPresentation(EditorPresentation.Target target,ResourceLocation id,EditorPresentation presentation){
        if(frozen)throw new IllegalStateException("Editor presentation registry frozen");
        if(PRESENTATIONS.putIfAbsent(new Key(Objects.requireNonNull(target),Objects.requireNonNull(id)),Objects.requireNonNull(presentation))!=null)
            throw new IllegalArgumentException("Duplicate editor presentation "+id);
    }
    public static Optional<EditorPresentation> presentation(EditorPresentation.Target target,ResourceLocation id){return Optional.ofNullable(PRESENTATIONS.get(new Key(target,id)));}
    public static synchronized void registerGizmo(ResourceLocation type,EditorGizmoProvider provider){
        if(frozen)throw new IllegalStateException("Editor gizmo registry frozen");
        if(PROVIDERS.putIfAbsent(Objects.requireNonNull(type),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate editor gizmo "+type);
    }
    /** Called after mod setup; no runtime provider replacement. */
    public static synchronized void freeze(){frozen=true;}
    public static Optional<EditorGizmoProvider> gizmo(ResourceLocation type){return Optional.ofNullable(PROVIDERS.get(type));}
    private MaplesEditorClientApi(){}
}
