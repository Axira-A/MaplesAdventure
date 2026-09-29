package dev.maplesadventure.api.editor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.maplesadventure.api.editor.EditorValue;
import java.util.Map;
import net.minecraft.world.phys.AABB;

/** Client-only experimental presentation hook. Pose is object-local, including yaw/pitch. No world writes. */
public interface EditorGizmoProvider {
    void render(PoseStack pose,VertexConsumer lines,Map<String,EditorValue> fields,boolean selected);
    default AABB bounds(Map<String,EditorValue> fields){return new AABB(-.25,-.25,-.25,.25,.25,.25);}
}
