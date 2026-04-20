package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.systems.hud.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NonNull;

public record ModulePersistentState(JsonObject settings, Boolean enabled, JsonObject hud) {
    public ModulePersistentState {
        settings = settings != null ? settings : new JsonObject();
        hud = hud != null ? hud : new JsonObject();
    }

    /**
     * Serializes a module's enabled flag and all setting values.
     *
     * @param module the module whose state should be captured
     * @return a JSON snapshot suitable for persistence
     */
    public static ModulePersistentState capture(Module module) {
        JsonObject map = new JsonObject();
        for (Setting<?> setting : module.getAllSettings()) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            JsonElement serialized = setting.serializeValue();
            if (serialized != null) {
                map.add(setting.getSettingKey(), serialized);
            }
        }
        JsonObject hud = getModuleJson(module);
        return new ModulePersistentState(map, module.isEnabled(), hud);
    }

    private static @NonNull JsonObject getModuleJson(Module module) {
        JsonObject hud = new JsonObject();
        if (module instanceof HudModule hudModule) {
            hud.addProperty("scale", hudModule.getHudState().getScale());
            hud.addProperty("anchor", hudModule.getHudState().getHudAnchor().name());
            hud.addProperty("anchor_offset_x", hudModule.getHudState().getAnchorOffsetX());
            hud.addProperty("anchor_offset_y", hudModule.getHudState().getAnchorOffsetY());
            hud.addProperty("last_layout_width", hudModule.getHudState().getLastLayoutWidth());
            hud.addProperty("last_layout_height", hudModule.getHudState().getLastLayoutHeight());
        }
        return hud;
    }

    private static void applySettingValue(Setting<?> setting, JsonElement jsonElement) {
        setting.deserializeValue(jsonElement);
    }

    /**
     * Applies stored values to the module; unknown JSON keys are ignored.
     *
     * @param module the module whose runtime state should be updated
     */
    public void applyTo(Module module) {
        if (enabled != null) {
            ModuleRegistry.INSTANCE.setModuleEnabled(module, enabled);
        }
        if (module instanceof HudModule hudModule) {
            if (hud.has("scale")) {
                hudModule.getHudState().setScale(hud.get("scale").getAsFloat());
            }
            if (hud.has("anchor")) {
                try {
                    hudModule.getHudState().setHudAnchor(HUDWidgetAnchor.valueOf(hud.get("anchor").getAsString()));
                } catch (IllegalArgumentException ignored) {
                    hudModule.getHudState().setHudAnchor(HUDWidgetAnchor.TOP_LEFT);
                }
            }
            if (hud.has("anchor_offset_x")) {
                hudModule.getHudState().setAnchorOffsetX(hud.get("anchor_offset_x").getAsFloat());
            }
            if (hud.has("anchor_offset_y")) {
                hudModule.getHudState().setAnchorOffsetY(hud.get("anchor_offset_y").getAsFloat());
            }
            if (hud.has("last_layout_width")) {
                float layoutWidth = hud.get("last_layout_width").getAsFloat();
                hudModule.getHudState().setLastLayoutWidth(layoutWidth);
                if (layoutWidth > 0f && Float.isFinite(layoutWidth)) {
                    hudModule.getHudState().setCommittedLayoutWidth(layoutWidth);
                }
            }
            if (hud.has("last_layout_height")) {
                float layoutHeight = hud.get("last_layout_height").getAsFloat();
                hudModule.getHudState().setLastLayoutHeight(layoutHeight);
                if (layoutHeight > 0f && Float.isFinite(layoutHeight)) {
                    hudModule.getHudState().setCommittedLayoutHeight(layoutHeight);
                }
            }
        }
        for (Setting<?> setting : module.getAllSettings()) {
            JsonElement jsonElement = settings.get(setting.getSettingKey());
            if (jsonElement == null || jsonElement.isJsonNull()) {
                continue;
            }
            applySettingValue(setting, jsonElement);
        }
    }
}
