package dev.maplesadventure.bonfire;

import dev.maplesadventure.api.bonfire.*;
import java.lang.reflect.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BonfirePublicApiContractTest {
    @Test void signaturesContainNoInternalOrOptionalModTypes() {
        for (Class<?> type : List.of(MaplesBonfireApi.class, MaplesBonfireFeatures.class, MaplesBonfireRef.class,
                MaplesBonfireView.class, MaplesBonfireContext.class, MaplesBonfireFeatureHandler.class,
                MaplesBonfireRestResetParticipant.class, MaplesBonfireRestCompletedEvent.class)) {
            for (Method method : type.getDeclaredMethods()) if (Modifier.isPublic(method.getModifiers())) {
                check(method.getGenericReturnType());
                for (Type parameter : method.getGenericParameterTypes()) check(parameter);
            }
            for (Constructor<?> constructor : type.getConstructors())
                for (Type parameter : constructor.getGenericParameterTypes()) check(parameter);
        }
        assertTrue(java.util.Arrays.stream(MaplesBonfireApi.class.getMethods()).noneMatch(m -> m.getName().equals("rest")));
    }
    private static void check(Type type) {
        String name = type.getTypeName();
        for (String forbidden : List.of("dev.maplesadventure.bonfire.", "progression.", "yesman.", "io.redspace.", "shouldersurfing.", "bonfires."))
            assertFalse(name.contains(forbidden), name);
    }
}
