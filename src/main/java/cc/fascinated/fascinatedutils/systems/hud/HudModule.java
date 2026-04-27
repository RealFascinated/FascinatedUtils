package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContentRenderer;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class HudModule extends Module implements HudRenderableModule {

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

        this.hudState.setHudAnchor(defaults.defaultAnchor());
        this.hudState.setAnchorOffsetX(defaults.defaultXOffset());
        this.hudState.setAnchorOffsetY(defaults.defaultYOffset());
        hudState.setPositionX(defaults.defaultXOffset());
        hudState.setPositionY(defaults.defaultYOffset());
        addSetting(padding);
        addSetting(textShadow);
    }

    protected HudModule(String widgetId, String displayName, float minWidth) {
        this(widgetId, displayName, minWidth, HudDefaults.builder().build() /* init with defaults */);
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
        return HudContentRenderer.prepare(glRenderer, content, this);
    }

    /**
     * How lines and blocks are aligned horizontally inside the panel, derived from the current anchor.
     */
    public HudAnchorContentAlignment.Horizontal hudContentHorizontalAlignment() {
        HudAnchorContentAlignment.Horizontal anchorHorizontalAlignment = HudAnchorContentAlignment.horizontal(hudState.getHudAnchor());
        if (anchorHorizontalAlignment != HudAnchorContentAlignment.Horizontal.CENTER) {
            return anchorHorizontalAlignment;
        }
        if (hudState.getHudAnchor() == HUDWidgetAnchor.BOTTOM) {
            return hudState.getAnchorOffsetX() < 0f ? HudAnchorContentAlignment.Horizontal.RIGHT : HudAnchorContentAlignment.Horizontal.LEFT;
        }
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    /**
     * How {@link HudContent.TextLines} are aligned horizontally within the padded panel. Override in a
     * {@link HudMiniMessageModule} subclass for a fixed alignment (default matches the previous always-centered text).
     *
     * @return horizontal alignment for each text line inside the inner band
     */
    public HudAnchorContentAlignment.Horizontal hudTextLineHorizontalAlignment() {
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    /**
     * How content is aligned vertically inside the panel, derived from the current anchor.
     */
    public HudAnchorContentAlignment.Vertical hudContentVerticalAlignment() {
        return HudAnchorContentAlignment.vertical(hudState.getHudAnchor());
    }


    @Override
    public void resetToDefault() {
        super.resetToDefault();

        this.setEnabled(defaults.defaultState());
        hudState.setScale(1.0f);
        this.hudState.setHudAnchor(defaults.defaultAnchor());
        this.hudState.setAnchorOffsetX(defaults.defaultXOffset());
        this.hudState.setAnchorOffsetY(defaults.defaultYOffset());
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
            float layoutWidth = hudJson.get("last_layout_width").getAsFloat();
            hudState.setLastLayoutWidth(layoutWidth);
            if (layoutWidth > 0f && Float.isFinite(layoutWidth)) {
                hudState.setCommittedLayoutWidth(layoutWidth);
            }
        }
        if (hudJson.has("last_layout_height")) {
            float layoutHeight = hudJson.get("last_layout_height").getAsFloat();
            hudState.setLastLayoutHeight(layoutHeight);
            if (layoutHeight > 0f && Float.isFinite(layoutHeight)) {
                hudState.setCommittedLayoutHeight(layoutHeight);
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
        float layoutWidth = layoutWidthForAnchoring();
        return layoutWidth * hudState.getScale();
    }

    public float getScaledHeight() {
        float layoutHeight = layoutHeightForAnchoring();
        return layoutHeight * hudState.getScale();
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
        HudAnchorContentAlignment.Horizontal horizontalAlignment = hudContentHorizontalAlignment();
        float resolvedX = 0f;
        float resolvedY = 0f;
        switch (hudState.getHudAnchor()) {
            case TOP_LEFT -> {
                resolvedX = offsetX;
                resolvedY = offsetY;
            }
            case TOP_RIGHT -> {
                resolvedX = canvasWidth - widgetWidth - offsetX;
                resolvedY = offsetY;
            }
            case BOTTOM_LEFT -> {
                resolvedX = offsetX;
                resolvedY = canvasHeight - widgetHeight - offsetY;
            }
            case BOTTOM_RIGHT -> {
                resolvedX = canvasWidth - widgetWidth - offsetX;
                resolvedY = canvasHeight - widgetHeight - offsetY;
            }
            case TOP -> {
                resolvedX = halfCanvasWidth - halfWidgetWidth + offsetX;
                resolvedY = offsetY;
            }
            case BOTTOM -> {
                resolvedX = switch (horizontalAlignment) {
                    case LEFT -> halfCanvasWidth + offsetX;
                    case RIGHT -> halfCanvasWidth - widgetWidth + offsetX;
                    case CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
                };
                resolvedY = canvasHeight - widgetHeight - offsetY;
            }
            case LEFT -> {
                resolvedX = offsetX;
                resolvedY = halfCanvasHeight - halfWidgetHeight + offsetY;
            }
            case RIGHT -> {
                resolvedX = canvasWidth - widgetWidth - offsetX;
                resolvedY = halfCanvasHeight - halfWidgetHeight + offsetY;
            }
            case CENTER -> {
                resolvedX = halfCanvasWidth - halfWidgetWidth + offsetX;
                resolvedY = halfCanvasHeight - halfWidgetHeight + offsetY;
            }
        }
        hudState.setPositionX(Mth.clamp(resolvedX, 0f, maxOffsetX));
        hudState.setPositionY(Mth.clamp(resolvedY, 0f, maxOffsetY));
    }

    public void captureNearestHudAnchorFromPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        hudState.setHudAnchor(getHudWidgetAnchor(canvasWidth, canvasHeight, widgetWidth, widgetHeight));
        syncHudAnchorOffsetsFromCurrentPosition(canvasWidth, canvasHeight);
    }

    public void syncHudAnchorOffsetsFromCurrentPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        HudAnchorContentAlignment.Horizontal horizontalAlignment = hudContentHorizontalAlignment();
        float positionX = hudState.getPositionX();
        float positionY = hudState.getPositionY();
        switch (hudState.getHudAnchor()) {
            case TOP_LEFT -> {
                hudState.setAnchorOffsetX(positionX);
                hudState.setAnchorOffsetY(positionY);
            }
            case TOP_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - positionX - widgetWidth);
                hudState.setAnchorOffsetY(positionY);
            }
            case BOTTOM_LEFT -> {
                hudState.setAnchorOffsetX(positionX);
                hudState.setAnchorOffsetY(canvasHeight - positionY - widgetHeight);
            }
            case BOTTOM_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - positionX - widgetWidth);
                hudState.setAnchorOffsetY(canvasHeight - positionY - widgetHeight);
            }
            case TOP -> {
                hudState.setAnchorOffsetX(positionX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(positionY);
            }
            case BOTTOM -> {
                float anchorOffsetX = switch (horizontalAlignment) {
                    case LEFT -> positionX - halfCanvasWidth;
                    case RIGHT -> positionX + widgetWidth - halfCanvasWidth;
                    case CENTER -> positionX + halfWidgetWidth - halfCanvasWidth;
                };
                hudState.setAnchorOffsetX(anchorOffsetX);
                hudState.setAnchorOffsetY(canvasHeight - positionY - widgetHeight);
            }
            case LEFT -> {
                hudState.setAnchorOffsetX(positionX);
                hudState.setAnchorOffsetY(positionY + halfWidgetHeight - halfCanvasHeight);
            }
            case RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - positionX - widgetWidth);
                hudState.setAnchorOffsetY(positionY + halfWidgetHeight - halfCanvasHeight);
            }
            case CENTER -> {
                hudState.setAnchorOffsetX(positionX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(positionY + halfWidgetHeight - halfCanvasHeight);
            }
        }
    }

    public final void recordHudContentSkipped() {
        if (hudState.getLastLayoutWidth() < 0f || hudState.getLastLayoutHeight() < 0f) {
            float defaultLayoutSize = Math.max(1f, minWidth);
            hudState.setLastLayoutWidth(defaultLayoutSize);
            hudState.setLastLayoutHeight(defaultLayoutSize);
            hudState.setCommittedLayoutWidth(defaultLayoutSize);
            hudState.setCommittedLayoutHeight(defaultLayoutSize);
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
        return getSetting(SliderSetting.class, SETTING_ROUNDING_RADIUS).map(setting -> setting.getValue().floatValue()).orElse(4f);
    }

    public void drawHUDPanelBackground(GuiRenderer glRenderer, float layoutWidth, float layoutHeight) {
        boolean showBg = getSetting(BooleanSetting.class, SETTING_BACKGROUND).map(BooleanSetting::isEnabled).orElse(false);
        boolean showBorder = getSetting(BooleanSetting.class, SETTING_SHOW_BORDER).map(BooleanSetting::isEnabled).orElse(false);
        float thickness = getSetting(SliderSetting.class, SETTING_BORDER_THICKNESS).map(setting -> setting.getValue().floatValue()).orElse(2f);
        float cornerRadius = getCornerRadius();
        int backgroundArgb = getSetting(ColorSetting.class, SETTING_BACKGROUND_COLOR).map(ColorSetting::getResolvedArgb).orElse(0x55000000);
        int borderArgb = getSetting(ColorSetting.class, SETTING_BORDER_COLOR).map(ColorSetting::getResolvedArgb).orElse(0xC0D0D7E1);
        HUDPanelBackground.drawPanelChrome(glRenderer, layoutWidth, layoutHeight, showBg, thickness, showBorder, cornerRadius, backgroundArgb, borderArgb);
    }

    @Override
    protected String settingTranslationKeyPrefix(Setting<?> setting) {
        return "fascinatedutils.module." + id;
    }

    /**
     * Subclasses override this to produce the content to be rendered.
     * The default {@link #prepareAndDraw} implementation delegates to this method
     * and uses {@link HudContentRenderer} to prepare the draw callback.
     *
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true if rendering in the HUD editor (for preview data)
     * @return the content to render, or {@code null} to skip rendering for this frame
     */
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

    private @NonNull HUDWidgetAnchor getHudWidgetAnchor(float canvasWidth, float canvasHeight, float widgetWidth, float widgetHeight) {
        float centerX = hudState.getPositionX() + widgetWidth * 0.5f;
        float centerY = hudState.getPositionY() + widgetHeight * 0.5f;
        HUDWidgetAnchor chosen = HUDWidgetAnchor.TOP_LEFT;
        float bestDistanceSquared = Float.MAX_VALUE;
        for (HUDWidgetAnchor candidate : HUDWidgetAnchor.values()) {
            float referenceX = candidate.referenceX(canvasWidth);
            float referenceY = candidate.referenceY(canvasHeight);
            float deltaX = centerX - referenceX;
            float deltaY = centerY - referenceY;
            float distanceSquared = deltaX * deltaX + deltaY * deltaY;
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                chosen = candidate;
            }
        }
        return chosen;
    }
}
