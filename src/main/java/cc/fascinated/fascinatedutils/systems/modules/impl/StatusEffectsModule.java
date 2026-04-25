package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.DateUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatusEffectsModule extends HudModule {
    private static final float ICON_SIZE = 18f;
    private static final float ICON_TEXT_GAP = 5f;
    private static final float TEXT_LINE_GAP = 1f;
    private static final float ROW_GAP = 2f;
    private static final Identifier PREVIEW_SPEED_ICON = Identifier.withDefaultNamespace("mob_effect/speed");
    private static final Identifier PREVIEW_POISON_ICON = Identifier.withDefaultNamespace("mob_effect/poison");
    private final BooleanSetting showAmplifier = BooleanSetting.builder().id("show_amplifier").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showDuration = BooleanSetting.builder().id("show_duration").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting flashTimeWhenEnding = SliderSetting.builder().id("flash_time_when_ending").defaultValue(10f).minValue(0f).maxValue(30f).step(1f).valueFormatter(value -> {
        int seconds = Math.round(value.floatValue());
        return seconds <= 0 ? "Off" : seconds + "s";
    }).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final EnumSetting<SortMode> sortMode = EnumSetting.<SortMode>builder().id("sort_mode").defaultValue(SortMode.REMAINING_TIME).valueFormatter(SortMode::displayName).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final EnumSetting<DisplayMode> displayMode = EnumSetting.<DisplayMode>builder().id("display_mode").defaultValue(DisplayMode.DETAILED).valueFormatter(DisplayMode::displayName).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final Minecraft minecraft = Minecraft.getInstance();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public StatusEffectsModule() {
        super("status_effects", "Status Effects", 56f);
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
        addSetting(showAmplifier);
        addSetting(showDuration);
        addSetting(flashTimeWhenEnding);
        addSetting(sortMode);
        addSetting(displayMode);
    }

    private static int whiteWithAlpha(float alpha) {
        int alphaChannel = Mth.clamp(Math.round(alpha * 255f), 0, 255);
        return (alphaChannel << 24) | 0x00FFFFFF;
    }

    private static Identifier getMobEffectSprite(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(ResourceKey::identifier).map(identifier -> identifier.withPrefix("mob_effect/")).orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    private static String formatEffectNameWithAmplifier(MobEffectInstance effectInstance, boolean includeAmplifier) {
        String effectName = effectInstance.getEffect().value().getDisplayName().getString();
        if (!includeAmplifier) {
            return effectName;
        }
        int amplifier = effectInstance.getAmplifier();
        if (amplifier <= 0) {
            return effectName;
        }

        int level = amplifier + 1;
        String levelKey = "enchantment.level." + level;
        String translatedLevel = Component.translatable(levelKey).getString();
        String levelText = translatedLevel.equals(levelKey) ? Integer.toString(level) : translatedLevel;
        return effectName + " " + levelText;
    }

    private static String compactText(String effectName, String durationText, boolean includeDuration) {
        if (!includeDuration || durationText == null || durationText.isBlank()) {
            return effectName;
        }
        return effectName + " " + durationText;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        List<EffectRow> effectRows = buildEffectRows(editorMode);
        if (effectRows.isEmpty()) {
            return null;
        }

        float fontHeight = glRenderer.getFontHeight();
        boolean showDurationValue = showDuration.isEnabled();
        boolean detailedLayout = displayMode.getValue() == DisplayMode.DETAILED && showDurationValue;
        float textBlockHeight = detailedLayout ? fontHeight * 2f + TEXT_LINE_GAP : fontHeight;
        float rowHeight = Math.max(ICON_SIZE, textBlockHeight);
        float maxRowWidth = 0f;
        float[] textWidths = new float[effectRows.size()];

        for (int rowIndex = 0; rowIndex < effectRows.size(); rowIndex++) {
            EffectRow effectRow = effectRows.get(rowIndex);
            float textWidth;
            if (detailedLayout) {
                float nameWidth = glRenderer.measureMiniMessageTextWidth(effectRow.nameText());
                float durationWidth = glRenderer.measureMiniMessageTextWidth(effectRow.durationText());
                textWidth = Math.max(nameWidth, durationWidth);
            }
            else {
                String compactText = compactText(effectRow.nameText(), effectRow.durationText(), showDurationValue);
                textWidth = glRenderer.measureMiniMessageTextWidth(compactText);
            }
            textWidths[rowIndex] = textWidth;
            maxRowWidth = Math.max(maxRowWidth, ICON_SIZE + ICON_TEXT_GAP + textWidth);
        }

        float contentHeight = effectRows.size() * rowHeight + Math.max(0f, effectRows.size() - 1f) * ROW_GAP;
        float padding = getPadding();
        float layoutWidth = Math.max(1f, Math.max(getMinWidth(), padding * 2f + maxRowWidth));
        float layoutHeight = Math.max(1f, padding * 2f + contentHeight);

        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        return () -> {
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);

            float innerWidth = layoutWidth - padding * 2f;
            float innerHeight = layoutHeight - padding * 2f;
            float cursorY = padding + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, contentHeight, hudContentVerticalAlignment());
            boolean rightAligned = hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            float sharedIconXWhenRight = padding + innerWidth - ICON_SIZE;
            float textRightEdgeWhenRight = sharedIconXWhenRight - ICON_TEXT_GAP;

            for (int rowIndex = 0; rowIndex < effectRows.size(); rowIndex++) {
                EffectRow effectRow = effectRows.get(rowIndex);

                float iconY = cursorY + (rowHeight - ICON_SIZE) * 0.5f;
                float textBlockY = cursorY + (rowHeight - textBlockHeight) * 0.5f;
                float nameY = textBlockY;
                float durationY = textBlockY + fontHeight + TEXT_LINE_GAP;

                float flashAlpha = effectRow.flashAlpha();
                if (rightAligned) {
                    float iconX = sharedIconXWhenRight;
                    if (detailedLayout) {
                        float nameX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(effectRow.nameText());
                        float durationX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(effectRow.durationText());
                        glRenderer.drawMiniMessageText(effectRow.nameText(), nameX, nameY, false);
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(effectRow.durationText(), durationX, durationY, false);
                        glRenderer.resetMultiplyAlpha();
                    }
                    else {
                        String compactLine = compactText(effectRow.nameText(), effectRow.durationText(), showDurationValue);
                        float compactX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(compactLine);
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(compactLine, compactX, nameY, false);
                        glRenderer.resetMultiplyAlpha();
                    }
                    glRenderer.drawSprite(effectRow.effectSprite(), iconX, iconY, ICON_SIZE, ICON_SIZE, whiteWithAlpha(1f));
                }
                else {
                    float rowWidth = ICON_SIZE + ICON_TEXT_GAP + textWidths[rowIndex];
                    float rowStartX = padding + HudAnchorLayout.horizontalOffsetInInnerBand(innerWidth, rowWidth, HudAnchorContentAlignment.Horizontal.LEFT);
                    float iconX = rowStartX;
                    float textX = rowStartX + ICON_SIZE + ICON_TEXT_GAP;
                    glRenderer.drawSprite(effectRow.effectSprite(), iconX, iconY, ICON_SIZE, ICON_SIZE, whiteWithAlpha(1f));
                    if (detailedLayout) {
                        glRenderer.drawMiniMessageText(effectRow.nameText(), textX, nameY, false);
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(effectRow.durationText(), textX, durationY, false);
                        glRenderer.resetMultiplyAlpha();
                    }
                    else {
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(compactText(effectRow.nameText(), effectRow.durationText(), showDurationValue), textX, nameY, false);
                        glRenderer.resetMultiplyAlpha();
                    }
                }

                cursorY += rowHeight;
                if (rowIndex < effectRows.size() - 1) {
                    cursorY += ROW_GAP;
                }
            }
        };
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    private float durationFlashAlpha(MobEffectInstance effectInstance) {
        if (effectInstance.isAmbient()) {
            return 1f;
        }
        int flashWindowTicks = Math.round(flashTimeWhenEnding.getValue().floatValue() * 20f);
        if (flashWindowTicks <= 0) {
            return 1f;
        }
        if (!effectInstance.endsWithin(flashWindowTicks)) {
            return 1f;
        }
        int remainingSeconds = effectInstance.getDuration() / 20;
        return (remainingSeconds % 2 == 0) ? 1f : 0f;
    }

    private List<EffectRow> buildEffectRows(boolean editorMode) {
        boolean includeAmplifier = showAmplifier.isEnabled();

        if (minecraft.player != null) {
            List<MobEffectInstance> activeEffects = new ArrayList<>(minecraft.player.getActiveEffects());
            activeEffects.removeIf(effectInstance -> !effectInstance.showIcon());
            if (!activeEffects.isEmpty()) {
                if (sortMode.getValue() == SortMode.ALPHABETICAL) {
                    activeEffects.sort(Comparator.comparing(effectInstance -> effectInstance.getEffect().value().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER));
                }
                else {
                    activeEffects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
                }
                List<EffectRow> rows = new ArrayList<>(activeEffects.size());
                for (MobEffectInstance effectInstance : activeEffects) {
                    String effectName = formatEffectNameWithAmplifier(effectInstance, includeAmplifier);
                    String durationText = DateUtils.formatDuration(effectInstance.getDuration());
                    rows.add(new EffectRow(getMobEffectSprite(effectInstance.getEffect()), effectName, durationText, durationFlashAlpha(effectInstance)));
                }
                return rows;
            }
        }

        if (editorMode) {
            return List.of(new EffectRow(PREVIEW_SPEED_ICON, includeAmplifier ? "Speed II" : "Speed", "01:25", 1f), new EffectRow(PREVIEW_POISON_ICON, includeAmplifier ? "Poison II" : "Poison", "00:37", 0.9f));
        }
        return List.of();
    }

    private enum SortMode {
        REMAINING_TIME("Remaining Time"), ALPHABETICAL("Alphabetical");

        private final String displayName;

        SortMode(String displayName) {
            this.displayName = displayName;
        }

        private String displayName() {
            return displayName;
        }
    }

    private enum DisplayMode {
        DETAILED("Detailed"), COMPACT("Compact");

        private final String displayName;

        DisplayMode(String displayName) {
            this.displayName = displayName;
        }

        private String displayName() {
            return displayName;
        }
    }

    private record EffectRow(Identifier effectSprite, String nameText, String durationText, float flashAlpha) {}
}
