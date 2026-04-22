package cc.fascinated.fascinatedutils.client.keybind;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class KeybindsWrapper {
    public static final KeybindsWrapper INSTANCE = new KeybindsWrapper();
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, FascinatedUtils.MOD_ID));

    private final Map<KeyMapping, Runnable> keybindCallbacks = new LinkedHashMap<>();

    /**
     * Dispatches every registered callback whose key was pressed this tick. Intended to run once per client tick from Fabric.
     */
    public static void dispatchRegisteredCallbacks() {
        for (Map.Entry<KeyMapping, Runnable> entry : INSTANCE.keybindCallbacks.entrySet()) {
            if (entry.getKey().consumeClick()) {
                entry.getValue().run();
            }
        }
    }

    public static KeyMapping registerCallbackKeybind(String keyTranslation, InputConstants.Type type, int key, KeyMapping.Category category, Runnable callback) {
        return INSTANCE.registerCallbackKeybindImpl(keyTranslation, type, key, category, callback);
    }

    /**
     * Registers a vanilla keybinding with no mod tick callback (use {@link KeyMapping#isDown()} etc.).
     *
     * @param keyTranslation translation key for the controls screen
     * @param type           key or scan type for the default binding
     * @param key            default key code or scan code
     * @param category       controls category tab
     * @return the registered keybinding
     */
    public static KeyMapping registerKeybind(String keyTranslation, InputConstants.Type type, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(keyTranslation, type, key, category));
    }

    private KeyMapping registerCallbackKeybindImpl(String keyTranslation, InputConstants.Type type, int key, KeyMapping.Category category, Runnable callback) {
        KeyMapping keyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(keyTranslation, type, key, category));
        keybindCallbacks.put(keyMapping, callback);
        return keyMapping;
    }
}
