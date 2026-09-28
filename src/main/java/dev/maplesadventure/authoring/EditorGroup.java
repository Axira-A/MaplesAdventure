package dev.maplesadventure.authoring;

import java.util.*;

public record EditorGroup(UUID id, String name, UUID parent, long revision) {
    public EditorGroup { Objects.requireNonNull(id); EditorLimits.name(name); if (revision < 0) throw new IllegalArgumentException("Invalid revision"); }
}
