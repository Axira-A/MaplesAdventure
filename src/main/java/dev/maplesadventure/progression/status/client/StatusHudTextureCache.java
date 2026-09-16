package dev.maplesadventure.progression.status.client;
import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import dev.maplesadventure.progression.status.StatusEffectType;

/** Regenerated once per resource reload. Registered textures are released before replacement. */
public final class StatusHudTextureCache implements ResourceManagerReloadListener {
    public static final ResourceLocation EMPTY=StatusVisualDefinition.id("textures/gui/status/buildup_empty.png");
    private static final ResourceLocation FULL=StatusVisualDefinition.id("textures/gui/status/buildup_full_red.png");
    public static final int FILL_X=18,FILL_Y=13,FILL_WIDTH=107,FILL_HEIGHT=6;
    private static final EnumMap<StatusEffectType,ResourceLocation> textures=new EnumMap<>(StatusEffectType.class);
    private static final EnumMap<StatusEffectType,StatusVisualDefinition> visuals=new EnumMap<>(StatusEffectType.class);
    public static ResourceLocation texture(StatusEffectType type) { return textures.get(type); }
    public static StatusVisualDefinition visual(StatusEffectType type) { return visuals.getOrDefault(type,StatusVisualDefinition.defaults(type)); }
    @Override public void onResourceManagerReload(ResourceManager manager) {
        var textureManager=Minecraft.getInstance().getTextureManager();
        textures.values().forEach(textureManager::release); textures.clear(); visuals.clear();
        try {
            manager.getResource(StatusVisualDefinition.id("status_visuals.json")).ifPresent(resource->{
                try(var reader=resource.openAsReader()) {
                    var json=JsonParser.parseReader(reader).getAsJsonObject();
                    for(var type:StatusEffectType.values()) if(json.has(type.id())) {
                        var value=json.getAsJsonObject(type.id());
                        visuals.put(type,new StatusVisualDefinition(Integer.parseInt(value.get("color").getAsString().replace("#",""),16),
                                ResourceLocation.parse(value.get("icon").getAsString())));
                    }
                } catch(Exception e) { dev.maplesadventure.MaplesAdventure.LOGGER.warn("Invalid status visual metadata",e); }
            });
            try(var stream=manager.getResourceOrThrow(FULL).open(); var original=NativeImage.read(stream)) {
                if(original.getWidth()!=128||original.getHeight()!=32) throw new IllegalArgumentException("Expected original 128x32 status texture");
                for(var type:StatusEffectType.values()) {
                    var pixels=new NativeImage(128,32,true);
                    for(int y=FILL_Y;y<FILL_Y+FILL_HEIGHT;y++) for(int x=FILL_X;x<FILL_X+FILL_WIDTH;x++) {
                        int p=original.getPixelRGBA(x,y),r=p&255,g=p>>>8&255,b=p>>>16&255;
                        // Only red fill, not the neutral frame. Preserve source alpha and intensity.
                        if(r>g*1.5&&r>b*1.5) pixels.setPixelRGBA(x,y,StatusHudMath.remap(p,visual(type).color()));
                    }
                    var texture=new DynamicTexture(pixels); texture.setFilter(false,false);
                    var id=StatusVisualDefinition.id("dynamic/status/"+type.id()); textureManager.register(id,texture); textures.put(type,id);
                }
            }
        } catch(Exception e) { dev.maplesadventure.MaplesAdventure.LOGGER.error("Status HUD assets unavailable; no substitute texture generated",e); }
    }
}
