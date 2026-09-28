package dev.maplesadventure.authoring;

/** Shared authoring and wire safety budgets. Not gameplay balance settings. */
public final class EditorLimits {
    public static final int SCENES = 256, OBJECTS = 4096, GROUPS = 1024, COMPONENTS = 32, DEPTH = 16;
    public static final int NAME = 128, STRING = 1024, ID = 256, FIELDS = 64;
    public static final int COMPONENT_BYTES = 16 * 1024, OBJECT_BYTES = 64 * 1024;
    public static final int SCENE_BYTES = 16 * 1024 * 1024, PAGE_BYTES = 256 * 1024;
    public static String name(String value) {
        if (value == null || value.isBlank() || value.length() > NAME || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("editor.maplesadventure.invalid_name");
        return value;
    }
    private EditorLimits() {}
}
