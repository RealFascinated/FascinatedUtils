package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.DateUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
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
    private static final float HORIZONTAL_PADDING = 5f;
    private static final float VERTICAL_PADDING = 4f;
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
    private final BooleanSetting showBackground = BooleanSetting.builder().id(SETTING_SHOW_BACKGROUND).defaultValue(true).translationKeyPath("fascinatedutils.module.show_hud_background").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBorder = BooleanSetting.builder().id(SETTING_SHOW_BORDER).defaultValue(false).translationKeyPath("fascinatedutils.module.show_border").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting borderThickness = SliderSetting.builder().id(SETTING_BORDER_THICKNESS).defaultValue(2f).minValue(1f).maxValue(3f).step(1f).translationKeyPath("fascinatedutils.module.border_thickness").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting padding = SliderSetting.builder().id(SETTING_PADDING).defaultValue(6f).minValue(0f).maxValue(16f).step(1f).translationKeyPath("fascinatedutils.module.padding").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public StatusEffectsModule() {
        super("status_effects", "Status Effects", 56f);
        addSetting(showBackground);
        addSetting(showBorder);
        addSetting(borderThickness);
        addSetting(padding);
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
        float layoutWidth = Math.max(1f, Math.max(getMinWidth(), HORIZONTAL_PADDING * 2f + maxRowWidth));
        float layoutHeight = Math.max(1f, VERTICAL_PADDING * 2f + contentHeight);

        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        return () -> {
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);

            float innerWidth = layoutWidth - HORIZONTAL_PADDING * 2f;
            float innerHeight = layoutHeight - VERTICAL_PADDING * 2f;
            float cursorY = VERTICAL_PADDING + HudAnchorLayout.verticalOffsetInInnerBand(innerHeight, contentHeight, hudContentVerticalAlignment());
            boolean rightAligned = hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            float sharedIconXWhenRight = HORIZONTAL_PADDING + innerWidth - ICON_SIZE;
            float textRightEdgeWhenRight = sharedIconXWhenRight - ICON_TEXT_GAP;

            for (int rowIndex = 0; rowIndex < effectRows.size(); rowIndex++) {
                EffectRow effectRow = effectRows.get(rowIndex);

                float iconY = cursorY + (rowHeight - ICON_SIZE) * 0.5f;
                float textBlockY = cursorY + (rowHeight - textBlockHeight) * 0.5f;
                float nameY = textBlockY;
                float durationY = textBlockY + fontHeight + TEXT_LINE_GAP;

                if (rightAligned) {
                    float iconX = sharedIconXWhenRight;
                    if (detailedLayout) {
                        float nameX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(effectRow.nameText());
                        float durationX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(effectRow.durationText());
                        glRenderer.drawMiniMessageText(effectRow.nameText(), nameX, nameY, false);
                        glRenderer.drawMiniMessageText(effectRow.durationText(), durationX, durationY, false);
                    }
                    else {
                        String compactLine = compactText(effectRow.nameText(), effectRow.durationText(), showDurationValue);
                        float compactX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(compactLine);
                        glRenderer.drawMiniMessageText(compactLine, compactX, nameY, false);
                    }
                    glRenderer.drawSprite(effectRow.effectSprite(), iconX, iconY, ICON_SIZE, ICON_SIZE, whiteWithAlpha(effectRow.iconAlpha()));
                }
                else {
                    float rowWidth = ICON_SIZE + ICON_TEXT_GAP + textWidths[rowIndex];
                    float rowStartX = HORIZONTAL_PADDING + HudAnchorLayout.horizontalOffsetInInnerBand(innerWidth, rowWidth, HudAnchorContentAlignment.Horizontal.LEFT);
                    float iconX = rowStartX;
                    float textX = rowStartX + ICON_SIZE + ICON_TEXT_GAP;
                    glRenderer.drawSprite(effectRow.effectSprite(), iconX, iconY, ICON_SIZE, ICON_SIZE, whiteWithAlpha(effectRow.iconAlpha()));
                    if (detailedLayout) {
                        glRenderer.drawMiniMessageText(effectRow.nameText(), textX, nameY, false);
                        glRenderer.drawMiniMessageText(effectRow.durationText(), textX, durationY, false);
                    }
                    else {
                        glRenderer.drawMiniMessageText(compactText(effectRow.nameText(), effectRow.durationText(), showDurationValue), textX, nameY, false);
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

    private float iconAlpha(MobEffectInstance effectInstance) {
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

        int remainingDuration = effectInstance.getDuration();
        float remainingFraction = Mth.clamp(remainingDuration / (float) flashWindowTicks, 0f, 1f);
        float usedFraction = 1f - remainingFraction;
        float baseAlpha = remainingFraction * 0.5f;
        float pulseAlpha = Mth.cos(remainingDuration * (float) Math.PI / 5.0f) * Mth.clamp(usedFraction * 0.25f, 0.0f, 0.25f);
        float alpha = baseAlpha + pulseAlpha;
        return Mth.clamp(alpha, 0.0f, 1.0f);
    }

    private List<EffectRow> buildEffectRows(boolean editorMode) {
        if (editorMode || minecraft.player == null) {
            boolean includeAmplifier = showAmplifier.isEnabled();
            return List.of(new EffectRow(PREVIEW_SPEED_ICON, includeAmplifier ? "Speed II" : "Speed", "01:25", 1f), new EffectRow(PREVIEW_POISON_ICON, includeAmplifier ? "Poison II" : "Poison", "00:37", 0.9f));
        }

        List<MobEffectInstance> activeEffects = new ArrayList<>(minecraft.player.getActiveEffects());
        activeEffects.removeIf(effectInstance -> !effectInstance.showIcon());
        if (sortMode.getValue() == SortMode.ALPHABETICAL) {
            activeEffects.sort(Comparator.comparing(effectInstance -> effectInstance.getEffect().value().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER));
        }
        else {
            activeEffects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        }

        List<EffectRow> rows = new ArrayList<>(activeEffects.size());
        boolean includeAmplifier = showAmplifier.isEnabled();
        for (MobEffectInstance effectInstance : activeEffects) {
            String effectName = formatEffectNameWithAmplifier(effectInstance, includeAmplifier);
            String durationText = DateUtils.formatDuration(effectInstance.getDuration());
            rows.add(new EffectRow(getMobEffectSprite(effectInstance.getEffect()), effectName, durationText, iconAlpha(effectInstance)));
        }
        return rows;
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

    private record EffectRow(Identifier effectSprite, String nameText, String durationText, float iconAlpha) {}
}
