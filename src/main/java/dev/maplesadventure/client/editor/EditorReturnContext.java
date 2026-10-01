package dev.maplesadventure.client.editor;

import dev.maplesadventure.authoring.MaplesScene;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Connection-local author UI only. Trusted camera position is retained by the server. */
public record EditorReturnContext(ResourceLocation dimension,ResourceLocation scene,UUID selected,
                                  boolean selectTool,boolean rotate,int hierarchyScroll,int inspectorScroll) {
    public UUID validSelection(MaplesScene authoritative){return selected!=null&&authoritative!=null&&scene.equals(authoritative.id())
            &&(authoritative.objects().containsKey(selected)||authoritative.groups().containsKey(selected))?selected:null;}
}
