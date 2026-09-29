package examples;

import com.mojang.blaze3d.vertex.*;
import dev.maplesadventure.api.editor.EditorValue;
import dev.maplesadventure.api.editor.client.*;
import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;

/** Invoke only from your CLIENT setup. Never reference this class in a common descriptor. */
public final class EditorGizmoExample {
    public static void register() {
        MaplesEditorClientApi.registerGizmo(EditorComponentExample.TYPE, new EditorGizmoProvider() {
            @Override public AABB bounds(Map<String, EditorValue> fields) {
                double r = fields.get("extent").number(); return new AABB(-r, -r, -r, r, r, r);
            }
            @Override public void render(PoseStack pose, VertexConsumer lines, Map<String, EditorValue> fields, boolean selected) {
                LevelRenderer.renderLineBox(pose, lines, bounds(fields), selected ? 1 : 0.4F, 0.8F, 1, 1);
            }
        });
    }
    private EditorGizmoExample() {}
}
