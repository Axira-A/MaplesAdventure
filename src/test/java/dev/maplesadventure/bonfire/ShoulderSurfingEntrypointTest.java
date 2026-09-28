package dev.maplesadventure.bonfire;

import com.google.gson.JsonParser;
import dev.maplesadventure.client.bonfire.BonfireCameraBridge;
import dev.maplesadventure.client.bonfire.BonfireCameraController;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Load the real, mutually incompatible APIs independently, just as either installed mod would. */
class ShoulderSurfingEntrypointTest {
    private static final String API = "com.github.exopandora.shouldersurfing.";
    private static final String ADAPTER = "dev.maplesadventure.integration.shouldersurfing.";

    @Test void legacyLoaderCanConstructAndRegisterItsOwnEntrypointWithoutAnyV5Types() throws Exception {
        try (var loader = loader("maples.shoulderLegacyJar")) {
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass(API + "api.event.IEventBus"));
            var callback = register(loader, false);
            Class<?> hook = loader.loadClass(API + "api.callback.ICameraRotationSetupCallback");
            Class<?> context = loader.loadClass(hook.getName() + "$CameraRotationSetupContext");
            Class<?> result = loader.loadClass(hook.getName() + "$CameraRotationSetupResult");
            Object rotation = result.getConstructor(float.class, float.class).newInstance(17f, 63f);
            // Outside a rest, never interfere with ordinary shoulder rotation.
            hook.getMethod("post", context, result).invoke(callback, null, rotation);
            assertEquals(17f, result.getMethod("getXRot").invoke(rotation));
            assertEquals(63f, result.getMethod("getYRot").invoke(rotation));
        }
    }

    @Test void modernLoaderPrefersPluralKeyAndRegistersOnlyTheModernAdapter() throws Exception {
        try (var loader = loader("maples.shoulderModernJar")) {
            var callback = register(loader, true);
            Class<?> hook = loader.loadClass(API + "api.client.event.handler.SetupCameraRotationEventHandler");
            Class<?> event = loader.loadClass(API + "api.client.event.SetupCameraRotationEvent");
            Class<?> vector = loader.loadClass(API + "api.math.Vec2f");
            Object rotation = vector.getConstructor(float.class, float.class).newInstance(17f, 63f);
            Object value = event.getConstructors()[0].newInstance(null, rotation, rotation, rotation, rotation);
            hook.getMethod("handle", event).invoke(callback, value);
            assertSame(rotation, event.getMethod("getResult").invoke(value));
        }
    }

    @Test void sharedControllerLoadsWithNeitherOptionalApiAndDoesNotQueryItOutsideARest() {
        var controller = new BonfireCameraController(() -> { fail("No active rest"); return false; },
                () -> { fail("No active rest"); return null; });
        assertNull(controller.rotationOverride());
    }

    private static Object register(ClassLoader loader, boolean modern) throws Exception {
        String name;
        try (var reader = new InputStreamReader(ShoulderSurfingEntrypointTest.class.getResourceAsStream(
                "/shouldersurfing_plugin.json"), StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(1, json.getAsJsonArray("entrypoints").size());
            name = modern ? json.getAsJsonArray("entrypoints").get(0).getAsString()
                    : json.get("entrypoint").getAsString();
        }
        Class<?> pluginApi = loader.loadClass(API + "api.plugin.IShoulderSurfingPlugin");
        Class<?> registry = loader.loadClass(API + (modern ? "api.event.IEventBus" : "api.plugin.IShoulderSurfingRegistrar"));
        Object plugin = loader.loadClass(name).getConstructor().newInstance();
        assertTrue(pluginApi.isInstance(plugin));
        List<Object> callbacks = new ArrayList<>();
        Object bus = Proxy.newProxyInstance(loader, new Class<?>[] {registry}, (proxy, method, args) -> {
            assertEquals(modern ? "register" : "registerCameraRotationSetupCallback", method.getName());
            callbacks.add(args[0]);
            return method.getReturnType() == void.class ? null : proxy;
        });
        var provider = BonfireCameraBridge.class.getDeclaredField("provider");
        provider.setAccessible(true);
        Object before = provider.get(null);
        try {
            // Invoke the installed version's interface, catching AbstractMethodError/linkage regressions.
            pluginApi.getMethod("register", registry).invoke(plugin, bus);
            assertEquals(1, callbacks.size());
            assertNotSame(before, provider.get(null));
            return callbacks.getFirst();
        } finally {
            provider.set(null, before);
        }
    }

    private static URLClassLoader loader(String jarProperty) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(Path.of(System.getProperty(jarProperty)).toUri().toURL());
        urls.add(BonfireCameraController.class.getProtectionDomain().getCodeSource().getLocation());
        for (String path : System.getProperty("maples.shoulderLegacyClasses").split(java.io.File.pathSeparator))
            urls.add(Path.of(path).toUri().toURL());
        return new URLClassLoader(urls.toArray(URL[]::new), ShoulderSurfingEntrypointTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith(API) && !name.startsWith(ADAPTER)) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> value = findLoadedClass(name);
                    if (value == null) value = findClass(name);
                    if (resolve) resolveClass(value);
                    return value;
                }
            }
        };
    }
}
