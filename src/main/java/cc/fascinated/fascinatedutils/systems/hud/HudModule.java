package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContentRenderer;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class HudModule extends Module implements HudRenderableModule {

    public static final String APPEARANCE_CATEGORY_DISPLAY_KEY = "Appearance";
    @Getter
    private final String id;
    @Getter
    private final float minWidth;
    @Getter
    private final ModuleHudState hudState = new ModuleHudState();
    private final BooleanSetting showBackground = BooleanSetting.builder().id("show_hud_background")

            .defaultValue(true).translationKeyPath("fascinatedutils.module.show_hud_background").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting roundedBackground = BooleanSetting.builder().id("round_hud_background")

            .defaultValue(false).translationKeyPath("fascinatedutils.module.round_hud_background").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBorder = BooleanSetting.builder().id("show_border")

            .defaultValue(false).translationKeyPath("fascinatedutils.module.show_border").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting borderThickness = SliderSetting.builder().id("border_thickness")

            .defaultValue(2f).minValue(1f).maxValue(3f).step(1f).translationKeyPath("fascinatedutils.module.border_thickness").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    private final SliderSetting roundingRadius = SliderSetting.builder().id("rounding_radius")

            .defaultValue(12f).minValue(4f).maxValue(24f).step(1f).translationKeyPath("fascinatedutils.module.rounding_radius").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    protected HudModule(String widgetId, String displayName, float minWidth) {
        super(displayName, ModuleCategory.HUD);
        this.id = widgetId;
        this.minWidth = minWidth;

        addSetting(showBackground);
        addSetting(roundedBackground);
        addSetting(showBorder);
        addSetting(borderThickness);
        addSetting(roundingRadius);
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
        return HudAnchorContentAlignment.horizontal(hudState.getHudAnchor());
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
        hudState.setVisible(false);
        hudState.setScale(1.0f);
        hudState.setHudAnchor(HUDWidgetAnchor.TOP_LEFT);
        hudState.setAnchorOffsetX(5f);
        hudState.setAnchorOffsetY(5f);
        hudState.setPositionX(5f);
        hudState.setPositionY(5f);
        hudState.setLastLayoutWidth(-1f);
        hudState.setLastLayoutHeight(-1f);
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
                resolvedX = halfCanvasWidth - halfWidgetWidth + offsetX;
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
                hudState.setAnchorOffsetX(positionX + halfWidgetWidth - halfCanvasWidth);
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

    public void drawHUDPanelBackground(GuiRenderer glRenderer, float layoutWidth, float layoutHeight) {
        HUDPanelBackground.drawPanelChrome(glRenderer, layoutWidth, layoutHeight, showBackground.isEnabled(), borderThickness.getValue().floatValue(), showBorder.isEnabled(), roundedBackground.isEnabled(), roundingRadius.getValue().floatValue());
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
