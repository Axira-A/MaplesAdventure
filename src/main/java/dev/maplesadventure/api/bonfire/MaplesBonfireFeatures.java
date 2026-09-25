package dev.maplesadventure.api.bonfire;

import net.minecraft.resources.ResourceLocation;

/** Stable feature IDs; an ID alone does not register gameplay or expose a menu action. */
public final class MaplesBonfireFeatures {
    public static final ResourceLocation LEVEL_UP = id("level_up");
    public static final ResourceLocation FLASK_ALLOCATION = id("flask_allocation");
    public static final ResourceLocation SPELL_MEMORY = id("spell_memory");
    public static final ResourceLocation REINFORCE = id("reinforce");
    public static final ResourceLocation WARP = id("warp");
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("maplesadventure", path);
    }
    private MaplesBonfireFeatures() {}
}
