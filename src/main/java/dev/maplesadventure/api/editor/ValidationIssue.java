package dev.maplesadventure.api.editor;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Experimental editor API: translation key and stable location, never an executable expression. */
public record ValidationIssue(Severity severity, UUID object, ResourceLocation component, String field, String message) {
    public enum Severity { INFO, WARNING, ERROR }
    public static ValidationIssue error(String message) { return new ValidationIssue(Severity.ERROR, null, null, "", message); }
}
