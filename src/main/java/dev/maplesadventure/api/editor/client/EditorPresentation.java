package dev.maplesadventure.api.editor.client;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Optional, client-only author presentation. Sentence tokens use {fieldId}; never executable code. */
public record EditorPresentation(String categoryKey,String descriptionKey,ResourceLocation icon,
                                 boolean advancedOnly,String sentenceKey) {
    public enum Target { COMPONENT, EVENT, CONDITION, ACTION }
    public EditorPresentation {
        Objects.requireNonNull(categoryKey);Objects.requireNonNull(descriptionKey);Objects.requireNonNull(sentenceKey);
        if(categoryKey.length()>256||descriptionKey.length()>256||sentenceKey.length()>1024)throw new IllegalArgumentException("Presentation limit");
    }
}
