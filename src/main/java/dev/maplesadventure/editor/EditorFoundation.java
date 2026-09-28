package dev.maplesadventure.editor;

import dev.maplesadventure.authoring.component.*;

/** Common registry only; contains no client or third-party gameplay types. */
public final class EditorFoundation {
    public static final ComponentRegistry COMPONENTS = BuiltinComponents.createRegistry();
    private EditorFoundation() {}
}
