package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContentRenderer;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.util.Mth;

import java.util.List;

public abstract class HudModule extends Module implements HudRenderableModule {
    public static final float UTILITY_WIDGET_MIN_WIDTH = 90f;

    public static final String APPEARANCE_CATEGORY_DISPLAY_KEY = "Appearance";
    public static final String SETTING_BACKGROUND = "background";
    public static final String SETTING_ROUNDED_CORNERS = "rounded_corners";
    public static final String SETTING_ROUNDING_RADIUS = "rounding_radius";
    public static final String SETTING_SHOW_BORDER = "show_border";
    public static final String SETTING_BORDER_THICKNESS = "border_thickness";
    public static final String SETTING_BACKGROUND_COLOR = "hud_background_color";
    public static final String SETTING_BORDER_COLOR = "hud_border_color";
    public static final String SETTING_PADDING = "hud_padding";
    public static final String SETTING_TEXT_SHADOW = "text_shadow";

    private final HudDefaults defaults;
    private final SliderSetting padding;
    private final BooleanSetting textShadow;

    @Getter
    private final String id;
    @Getter
    private final float minWidth;
    @Getter
    private final ModuleHudState hudState = new ModuleHudState();

    protected HudModule(String widgetId, String displayName, float minWidth, HudDefaults defaults) {
        super(displayName, ModuleCategory.HUD);
        this.defaults = defaults;
        this.id = widgetId;
        this.minWidth = minWidth;
        this.padding = HudWidgetAppearanceBuilders.padding().defaultValue(defaults.defaultPadding()).build();
        this.textShadow = HudWidgetAppearanceBuilders.textShadow().build();

        hudState.setHudAnchor(defaults.defaultAnchor());
        hudState.setAnchorOffsetX(defaults.defaultXOffset());
        hudState.setAnchorOffsetY(defaults.defaultYOffset());
        hudState.setPositionX(defaults.defaultXOffset());
        hudState.setPositionY(defaults.defaultYOffset());
        addSetting(padding);
        addSetting(textShadow);
    }

    protected HudModule(String widgetId, String displayName, float minWidth) {
        this(widgetId, displayName, minWidth, HudDefaults.builder().build());
    }

    @Override
    public String getName() {
        return getDisplayName();
    }

    @Override
    public Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        HudContent content = produceContent(deltaSeconds, editorMode);
        if (content == null) {
            return null;
        }
        return HudContentRenderer.prepare(glRenderer, content, this, editorMode);
    }

    public HudAnchorContentAlignment.Horizontal hudContentHorizontalAlignment() {
        HudAnchorContentAlignment.Horizontal anchorAlignment = HudAnchorContentAlignment.horizontal(hudState.getHudAnchor());
        if (anchorAlignment != HudAnchorContentAlignment.Horizontal.CENTER) {
            return anchorAlignment;
        }
        if (hudState.getHudAnchor() == HUDWidgetAnchor.BOTTOM) {
            return hudState.getAnchorOffsetX() < 0f ? HudAnchorContentAlignment.Horizontal.RIGHT : HudAnchorContentAlignment.Horizontal.LEFT;
        }
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    public HudAnchorContentAlignment.Horizontal hudTextLineHorizontalAlignment() {
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    public HudAnchorContentAlignment.Vertical hudContentVerticalAlignment() {
        return HudAnchorContentAlignment.vertical(hudState.getHudAnchor());
    }

    @Override
    public void resetToDefault() {
        super.resetToDefault();
        this.setEnabled(defaults.defaultState());
        hudState.setScale(1.0f);
        hudState.setHudAnchor(defaults.defaultAnchor());
        hudState.setAnchorOffsetX(defaults.defaultXOffset());
        hudState.setAnchorOffsetY(defaults.defaultYOffset());
        hudState.setPositionX(defaults.defaultXOffset());
        hudState.setPositionY(defaults.defaultYOffset());
        hudState.setLastLayoutWidth(-1f);
        hudState.setLastLayoutHeight(-1f);
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = super.serialize(gson).getAsJsonObject();
        JsonObject hudJson = new JsonObject();
        hudJson.addProperty("scale", hudState.getScale());
        hudJson.addProperty("anchor", hudState.getHudAnchor().name());
        hudJson.addProperty("anchor_offset_x", hudState.getAnchorOffsetX());
        hudJson.addProperty("anchor_offset_y", hudState.getAnchorOffsetY());
        hudJson.addProperty("last_layout_width", hudState.getLastLayoutWidth());
        hudJson.addProperty("last_layout_height", hudState.getLastLayoutHeight());
        root.add("hud", hudJson);
        return root;
    }

    @Override
    public Module deserialize(JsonElement data, Gson gson) {
        super.deserialize(data, gson);
        if (!data.isJsonObject()) {
            return this;
        }

        JsonObject root = data.getAsJsonObject();
        if (!root.has("hud") || !root.get("hud").isJsonObject()) {
            return this;
        }

        JsonObject hudJson = root.get("hud").getAsJsonObject();
        if (hudJson.has("scale")) {
            hudState.setScale(hudJson.get("scale").getAsFloat());
        }
        if (hudJson.has("anchor")) {
            try {
                hudState.setHudAnchor(HUDWidgetAnchor.valueOf(hudJson.get("anchor").getAsString()));
            } catch (IllegalArgumentException ignored) {
                hudState.setHudAnchor(HUDWidgetAnchor.TOP_LEFT);
            }
        }
        if (hudJson.has("anchor_offset_x")) {
            hudState.setAnchorOffsetX(hudJson.get("anchor_offset_x").getAsFloat());
        }
        if (hudJson.has("anchor_offset_y")) {
            hudState.setAnchorOffsetY(hudJson.get("anchor_offset_y").getAsFloat());
        }
        if (hudJson.has("last_layout_width")) {
            float w = hudJson.get("last_layout_width").getAsFloat();
            hudState.setLastLayoutWidth(w);
            if (w > 0f && Float.isFinite(w)) {
                hudState.setCommittedLayoutWidth(w);
            }
        }
        if (hudJson.has("last_layout_height")) {
            float h = hudJson.get("last_layout_height").getAsFloat();
            hudState.setLastLayoutHeight(h);
            if (h > 0f && Float.isFinite(h)) {
                hudState.setCommittedLayoutHeight(h);
            }
        }
        return this;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return SettingCategoryGrouper.topLevelInRegistrationOrder(getAllSettings());
    }

    @Override
    public List<SettingCategory> getSettingCategories() {
        return SettingCategoryGrouper.categoriesInRegistrationOrder(getAllSettings());
    }

    public float getScaledWidth() {
        return layoutWidthForAnchoring() * hudState.getScale();
    }

    public float getScaledHeight() {
        return layoutHeightForAnchoring() * hudState.getScale();
    }

    public boolean containsPoint(float pointerX, float pointerY) {
        return pointerX >= hudState.getPositionX() && pointerX <= hudState.getPositionX() + getScaledWidth() && pointerY >= hudState.getPositionY() && pointerY <= hudState.getPositionY() + getScaledHeight();
    }

    public void applyHudAnchorToPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float maxOffsetX = Math.max(0f, canvasWidth - widgetWidth);
        float maxOffsetY = Math.max(0f, canvasHeight - widgetHeight);
        float offsetX = hudState.getAnchorOffsetX();
        float offsetY = hudState.getAnchorOffsetY();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        HudAnchorContentAlignment.Horizontal hAlign = hudContentHorizontalAlignment();

        float resolvedX = switch (hudState.getHudAnchor()) {
            case TOP_LEFT, LEFT -> offsetX;
            case TOP_RIGHT, RIGHT -> canvasWidth - widgetWidth - offsetX;
            case BOTTOM_LEFT -> offsetX;
            case BOTTOM_RIGHT -> canvasWidth - widgetWidth - offsetX;
            case TOP, CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
            case BOTTOM -> switch (hAlign) {
                case LEFT -> halfCanvasWidth + offsetX;
                case RIGHT -> halfCanvasWidth - widgetWidth + offsetX;
                case CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
            };
        };

        float resolvedY = switch (hudState.getHudAnchor()) {
            case TOP_LEFT, TOP_RIGHT, TOP -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM -> canvasHeight - widgetHeight - offsetY;
            case LEFT, RIGHT -> halfCanvasHeight - halfWidgetHeight + offsetY;
            case CENTER -> halfCanvasHeight - halfWidgetHeight + offsetY;
        };

        hudState.setPositionX(Mth.clamp(resolvedX, 0f, maxOffsetX));
        hudState.setPositionY(Mth.clamp(resolvedY, 0f, maxOffsetY));
    }

    public void captureNearestHudAnchorFromPosition(float canvasWidth, float canvasHeight) {
        hudState.setHudAnchor(nearestAnchor(canvasWidth, canvasHeight));
        syncHudAnchorOffsetsFromCurrentPosition(canvasWidth, canvasHeight);
    }

    public void syncHudAnchorOffsetsFromCurrentPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        HudAnchorContentAlignment.Horizontal hAlign = hudContentHorizontalAlignment();
        float posX = hudState.getPositionX();
        float posY = hudState.getPositionY();

        switch (hudState.getHudAnchor()) {
            case TOP_LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(posY);
            }
            case TOP_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(posY);
            }
            case BOTTOM_LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case BOTTOM_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case TOP -> {
                hudState.setAnchorOffsetX(posX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(posY);
            }
            case BOTTOM -> {
                float ox = switch (hAlign) {
                    case LEFT -> posX - halfCanvasWidth;
                    case RIGHT -> posX + widgetWidth - halfCanvasWidth;
                    case CENTER -> posX + halfWidgetWidth - halfCanvasWidth;
                };
                hudState.setAnchorOffsetX(ox);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
            case RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
            case CENTER -> {
                hudState.setAnchorOffsetX(posX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
        }
    }

    public final void recordHudContentSkipped() {
        if (hudState.getLastLayoutWidth() < 0f || hudState.getLastLayoutHeight() < 0f) {
            float fallback = Math.max(1f, minWidth);
            hudState.setLastLayoutWidth(fallback);
            hudState.setLastLayoutHeight(fallback);
            hudState.setCommittedLayoutWidth(fallback);
            hudState.setCommittedLayoutHeight(fallback);
        }
    }

    public float getPadding() {
        return padding.getValue().floatValue();
    }

    public boolean isTextShadowEnabled() {
        return textShadow.isEnabled();
    }

    public float getCornerRadius() {
        boolean rounded = getSetting(BooleanSetting.class, SETTING_ROUNDED_CORNERS).map(BooleanSetting::isEnabled).orElse(false);
        if (!rounded) {
            return 0f;
        }
        return getSetting(SliderSetting.class, SETTING_ROUNDING_RADIUS).map(s -> s.getValue().floatValue()).orElse(4f);
    }

    public void drawHUDPanelBackground(GuiRenderer glRenderer, float layoutWidth, float layoutHeight, boolean editorMode) {
        boolean showBg = getSetting(BooleanSetting.class, SETTING_BACKGROUND).map(BooleanSetting::isEnabled).orElse(false);
        boolean showBorder = getSetting(BooleanSetting.class, SETTING_SHOW_BORDER).map(BooleanSetting::isEnabled).orElse(false);
        float thickness = getSetting(SliderSetting.class, SETTING_BORDER_THICKNESS).map(s -> s.getValue().floatValue()).orElse(2f);
        float cornerRadius = getCornerRadius();
        int backgroundArgb = getSetting(ColorSetting.class, SETTING_BACKGROUND_COLOR).map(ColorSetting::getResolvedArgb).orElse(0x55000000);
        int borderArgb = getSetting(ColorSetting.class, SETTING_BORDER_COLOR).map(ColorSetting::getResolvedArgb).orElse(0xC0D0D7E1);
        HUDPanelBackground.drawPanelChrome(glRenderer, layoutWidth, layoutHeight, showBg, thickness, showBorder, cornerRadius, backgroundArgb, borderArgb, editorMode);
    }

    @Override
    protected String settingTranslationKeyPrefix(Setting<?> setting) {
        return "fascinatedutils.module." + id;
    }

    protected abstract HudContent produceContent(float deltaSeconds, boolean editorMode);

    private float layoutWidthForAnchoring() {
        if (hudState.getCommittedLayoutWidth() >= 0f) {
            return hudState.getCommittedLayoutWidth();
        }
        if (hudState.getLastLayoutWidth() >= 0f) {
            return hudState.getLastLayoutWidth();
        }
        return minWidth;
    }

    private float layoutHeightForAnchoring() {
        if (hudState.getCommittedLayoutHeight() >= 0f) {
            return hudState.getCommittedLayoutHeight();
        }
        if (hudState.getLastLayoutHeight() >= 0f) {
            return hudState.getLastLayoutHeight();
        }
        return minWidth;
    }

    private HUDWidgetAnchor nearestAnchor(float canvasWidth, float canvasHeight) {
        float centerX = hudState.getPositionX() + getScaledWidth() * 0.5f;
        float centerY = hudState.getPositionY() + getScaledHeight() * 0.5f;
        HUDWidgetAnchor chosen = HUDWidgetAnchor.TOP_LEFT;
        float bestDist = Float.MAX_VALUE;
        for (HUDWidgetAnchor candidate : HUDWidgetAnchor.values()) {
            float dx = centerX - candidate.referenceX(canvasWidth);
            float dy = centerY - candidate.referenceY(canvasHeight);
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                chosen = candidate;
            }
        }
        return chosen;
    }
}