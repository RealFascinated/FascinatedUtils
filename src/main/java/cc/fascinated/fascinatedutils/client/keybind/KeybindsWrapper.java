package cc.fascinated.fascinatedutils.client.keybind;

import cc.fascinated.fascinatedutils.AlumiteMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class KeybindsWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeybindsWrapper.class);
    public static final KeybindsWrapper INSTANCE = new KeybindsWrapper();
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, AlumiteMod.MOD_ID));

    private final Map<KeyMapping, Runnable> keybindCallbacks = new LinkedHashMap<>();

    /**
     * Dispatches every registered callback whose key was pressed this tick. Intended to run once per client tick from Fabric.
     */
    public static void dispatchRegisteredCallbacks() {
        for (Map.Entry<KeyMapping, Runnable> entry : INSTANCE.keybindCallbacks.entrySet()) {
            try {
                if (entry.getKey().consumeClick()) {
                    entry.getValue().run();
                }
            } catch (Throwable throwable) {
                LOGGER.error("Keybind callback failed for {}", entry.getKey().getName(), throwable);
            }
        }
    }

    public static KeyMapping registerCallbackKeybind(String keyTranslation, InputConstants.Type type, int key, KeyMapping.Category category, Runnable callback) {
        KeyMapping keyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(keyTranslation, type, key, category));
        INSTANCE.keybindCallbacks.put(keyMapping, callback);
        return keyMapping;
    }

    /**
     * Registers a vanilla keybinding with no mod tick callback (use {@link KeyMapping#isDown()} etc.).
     */
    public static KeyMapping registerKeybind(String keyTranslation, InputConstants.Type type, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(keyTranslation, type, key, category));
    }
}
