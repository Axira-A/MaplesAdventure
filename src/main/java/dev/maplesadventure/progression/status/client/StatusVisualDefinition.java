package dev.maplesadventure.progression.status.client;
import dev.maplesadventure.progression.status.StatusEffectType;
import net.minecraft.resources.ResourceLocation;
public record StatusVisualDefinition(int color,ResourceLocation icon) {
    public static StatusVisualDefinition defaults(StatusEffectType type) {
        return switch(type) {
            case BLEED -> new StatusVisualDefinition(0xA32622,id("textures/gui/infusion/blood.png"));
            case POISON -> new StatusVisualDefinition(0x789B36,id("textures/gui/infusion/poison.png"));
            case SCARLET_ROT -> new StatusVisualDefinition(0xB94B32,id("textures/gui/status/scarlet_rot.png"));
            case FROSTBITE -> new StatusVisualDefinition(0x69B8D4,id("textures/gui/status/frost.png"));
            case SLEEP -> new StatusVisualDefinition(0xAD8BCF,id("textures/gui/status/sleep.png"));
            case MADNESS -> new StatusVisualDefinition(0xE9AA24,id("textures/gui/status/madness.png"));
            case DEATH_BLIGHT -> new StatusVisualDefinition(0x8E9490,id("textures/gui/status/death_blight.png"));
        };
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("maplesadventure",path); }
}
