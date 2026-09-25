package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import com.google.gson.*;
class StatusDamageSourceTest {
    @Test void percentageSourcesBypassArmorAndQuietDotsHaveNoImpact() throws Exception {
        var root=Path.of("src/main/resources/data/minecraft/tags/damage_type");
        for(String tag:new String[]{"bypasses_armor","bypasses_resistance","bypasses_enchantments","bypasses_cooldown","no_knockback"}) {
            var values=JsonParser.parseString(Files.readString(root.resolve(tag+".json"))).getAsJsonObject().getAsJsonArray("values");
            for(String type:new String[]{"bleed","poison","scarlet_rot","frostbite","madness","death_blight"}) assertTrue(values.contains(new JsonPrimitive("maplesadventure:"+type)));
        }
        String impact=Files.readString(root.resolve("no_impact.json"));assertTrue(impact.contains("poison"));assertTrue(impact.contains("scarlet_rot"));
    }
}
