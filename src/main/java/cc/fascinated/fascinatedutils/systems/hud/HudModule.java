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
        hudState.setAnchorOffsetX(0.005f);
        hudState.setAnchorOffsetY(0.005f);
        hudState.setPositionX(5f);
        hudState.setPositionY(5f);
        hudState.setLastLayoutWidth(-1f);
        hudState.setLastLayoutHeight(-1f);
        hudState.setProportionalOffsets(true);
        hudState.setNeedsProportionalMigration(false);
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
        float layoutWidth = hudState.getCommittedLayoutWidth() >= 0f ? hudState.getCommittedLayoutWidth() : minWidth;
        return layoutWidth * hudState.getScale();
    }

    public float getScaledHeight() {
        float layoutHeight = hudState.getCommittedLayoutHeight() >= 0f ? hudState.getCommittedLayoutHeight() : minWidth;
        return layoutHeight * hudState.getScale();
    }

    public boolean containsPoint(float pointerX, float pointerY) {
        return pointerX >= hudState.getPositionX() && pointerX <= hudState.getPositionX() + getScaledWidth() && pointerY >= hudState.getPositionY() && pointerY <= hudState.getPositionY() + getScaledHeight();
    }

    public boolean closeButtonContainsPoint(float pointerX, float pointerY) {
        float buttonSize = 10f;
        float buttonPad = 2f;
        float scaledWidth = getScaledWidth();
        float buttonX = hudState.getPositionX() + scaledWidth - buttonSize - buttonPad;
        float buttonY = hudState.getPositionY() + buttonPad;
        return pointerX >= buttonX && pointerX <= buttonX + buttonSize && pointerY >= buttonY && pointerY <= buttonY + buttonSize;
    }

    public void applyHudAnchorToPosition(float canvasWidth, float canvasHeight) {
        if (hudState.isNeedsProportionalMigration()) {
            migrateToProportionalOffsets(canvasWidth, canvasHeight);
        }
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float maxOffsetX = Math.max(0f, canvasWidth - widgetWidth);
        float maxOffsetY = Math.max(0f, canvasHeight - widgetHeight);
        float offsetX = hudState.getAnchorOffsetX() * canvasWidth;
        float offsetY = hudState.getAnchorOffsetY() * canvasHeight;
        float resolvedX = offsetX;
        float resolvedY = offsetY;
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
                resolvedX = (canvasWidth - widgetWidth) * 0.5f + offsetX;
                resolvedY = offsetY;
            }
            case BOTTOM -> {
                resolvedX = (canvasWidth - widgetWidth) * 0.5f + offsetX;
                resolvedY = canvasHeight - widgetHeight - offsetY;
            }
            case LEFT -> {
                resolvedX = offsetX;
                resolvedY = (canvasHeight - widgetHeight) * 0.5f + offsetY;
            }
            case RIGHT -> {
                resolvedX = canvasWidth - widgetWidth - offsetX;
                resolvedY = (canvasHeight - widgetHeight) * 0.5f + offsetY;
            }
            case CENTER -> {
                resolvedX = (canvasWidth - widgetWidth) * 0.5f + offsetX;
                resolvedY = (canvasHeight - widgetHeight) * 0.5f + offsetY;
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
        float safeCanvasW = Math.max(1f, canvasWidth);
        float safeCanvasH = Math.max(1f, canvasHeight);
        switch (hudState.getHudAnchor()) {
            case TOP_LEFT -> {
                hudState.setAnchorOffsetX(hudState.getPositionX() / safeCanvasW);
                hudState.setAnchorOffsetY(hudState.getPositionY() / safeCanvasH);
            }
            case TOP_RIGHT -> {
                hudState.setAnchorOffsetX((canvasWidth - widgetWidth - hudState.getPositionX()) / safeCanvasW);
                hudState.setAnchorOffsetY(hudState.getPositionY() / safeCanvasH);
            }
            case BOTTOM_LEFT -> {
                hudState.setAnchorOffsetX(hudState.getPositionX() / safeCanvasW);
                hudState.setAnchorOffsetY((canvasHeight - widgetHeight - hudState.getPositionY()) / safeCanvasH);
            }
            case BOTTOM_RIGHT -> {
                hudState.setAnchorOffsetX((canvasWidth - widgetWidth - hudState.getPositionX()) / safeCanvasW);
                hudState.setAnchorOffsetY((canvasHeight - widgetHeight - hudState.getPositionY()) / safeCanvasH);
            }
            case TOP -> {
                hudState.setAnchorOffsetX((hudState.getPositionX() - (canvasWidth - widgetWidth) * 0.5f) / safeCanvasW);
                hudState.setAnchorOffsetY(hudState.getPositionY() / safeCanvasH);
            }
            case BOTTOM -> {
                hudState.setAnchorOffsetX((hudState.getPositionX() - (canvasWidth - widgetWidth) * 0.5f) / safeCanvasW);
                hudState.setAnchorOffsetY((canvasHeight - widgetHeight - hudState.getPositionY()) / safeCanvasH);
            }
            case LEFT -> {
                hudState.setAnchorOffsetX(hudState.getPositionX() / safeCanvasW);
                hudState.setAnchorOffsetY((hudState.getPositionY() - (canvasHeight - widgetHeight) * 0.5f) / safeCanvasH);
            }
            case RIGHT -> {
                hudState.setAnchorOffsetX((canvasWidth - widgetWidth - hudState.getPositionX()) / safeCanvasW);
                hudState.setAnchorOffsetY((hudState.getPositionY() - (canvasHeight - widgetHeight) * 0.5f) / safeCanvasH);
            }
            case CENTER -> {
                hudState.setAnchorOffsetX((hudState.getPositionX() - (canvasWidth - widgetWidth) * 0.5f) / safeCanvasW);
                hudState.setAnchorOffsetY((hudState.getPositionY() - (canvasHeight - widgetHeight) * 0.5f) / safeCanvasH);
            }
        }
        hudState.setProportionalOffsets(true);
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

    /**
     * Converts legacy pixel-based anchor offsets to proportional fractions of the canvas.
     * Called once on first render after loading a config without proportional offsets.
     */
    private void migrateToProportionalOffsets(float canvasWidth, float canvasHeight) {
        float safeCanvasW = Math.max(1f, canvasWidth);
        float safeCanvasH = Math.max(1f, canvasHeight);
        hudState.setAnchorOffsetX(hudState.getAnchorOffsetX() / safeCanvasW);
        hudState.setAnchorOffsetY(hudState.getAnchorOffsetY() / safeCanvasH);
        hudState.setProportionalOffsets(true);
        hudState.setNeedsProportionalMigration(false);
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
