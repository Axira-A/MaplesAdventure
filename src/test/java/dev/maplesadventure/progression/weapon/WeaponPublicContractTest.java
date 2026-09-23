package dev.maplesadventure.progression.weapon;

import com.google.gson.JsonParser;
import java.nio.file.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponPublicContractTest {
    private static com.google.gson.JsonObject example(String directory) throws Exception {
        var path=Path.of("docs/integration/examples/datapack/data/example/maplesadventure",directory,"holy_blade.json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    @Test void officialHolyBladeUsesRealParsers() throws Exception {
        var id=ResourceLocation.parse("example:holy_blade");
        var requirement=WeaponRequirementRules.parse(id,example("weapon_requirements")).profile();
        var scaling=WeaponScalingRules.parse(id,example("weapon_scaling")).profile();
        var damage=WeaponDamageProfileRules.parse(id,example("weapon_damage_profiles")).profile();
        var eligibility=WeaponInfusionEligibilityRules.parse(id,example("weapon_infusion_eligibility"));
        assertEquals(12,requirement.strength()); assertEquals(24,requirement.faith());
        assertEquals(.35,scaling.strength()); assertEquals(.20,scaling.dexterity()); assertEquals(.80,scaling.faith());
        assertEquals(2,damage.components().size());
        assertEquals(.65,damage.components().get(0).baseRatio());
        assertEquals(WeaponDamageChannel.HOLY,damage.components().get(1).channel());
        assertEquals(.80,damage.components().get(1).scaling(scaling).faith());
        assertEquals(0,damage.components().get(0).scaling(scaling).faith());
        assertTrue(eligibility.allowed().contains(ResourceLocation.parse("maplesadventure:sacred")));
    }
}
