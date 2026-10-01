package dev.maplesadventure.authoring;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Author-facing name only. Runtime values remain exclusively in GameFlagService. */
public record FlagDefinition(ResourceLocation id, String name) {
    public FlagDefinition { Objects.requireNonNull(id);if(id.toString().length()>EditorLimits.ID)throw new IllegalArgumentException("editor.maplesadventure.limit");name=normalizeName(name); }
    public static String normalizeName(String value){EditorLimits.name(value);return EditorLimits.name(java.text.Normalizer.normalize(value.strip(),java.text.Normalizer.Form.NFC));}
}
