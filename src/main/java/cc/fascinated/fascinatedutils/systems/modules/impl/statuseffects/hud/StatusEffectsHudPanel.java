package cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.hud;

import cc.fascinated.fascinatedutils.common.DateUtils;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule.DisplayMode;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule.SortMode;
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

public class StatusEffectsHudPanel extends HudPanel {

    private static final float ICON_SIZE = 18f;
    private static final float ICON_TEXT_GAP = 5f;
    private static final float TEXT_LINE_GAP = 1f;
    private static final float ROW_GAP = 2f;
    private static final Identifier PREVIEW_SPEED_ICON = Identifier.withDefaultNamespace("mob_effect/speed");
    private static final Identifier PREVIEW_POISON_ICON = Identifier.withDefaultNamespace("mob_effect/poison");

    private final StatusEffectsModule statusEffectsModule;
    private final Minecraft minecraft = Minecraft.getInstance();

    public StatusEffectsHudPanel(StatusEffectsModule statusEffectsModule) {
        super(statusEffectsModule, "status_effects", 56f);
        this.statusEffectsModule = statusEffectsModule;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        List<EffectRow> effectRows = resolveEffectRows(editorMode);
        if (effectRows.isEmpty()) {
            return null;
        }

        float fontHeight = glRenderer.getFontHeight();
        boolean showDurationValue = statusEffectsModule.effectsShowDurationSetting();
        boolean detailedLayout = statusEffectsModule.effectsDisplayMode() == DisplayMode.DETAILED && showDurationValue;
        float textBlockHeight = detailedLayout ? fontHeight * 2f + TEXT_LINE_GAP : fontHeight;
        float rowHeight = Math.max(ICON_SIZE, textBlockHeight);
        float maxRowWidth = 0f;
        float[] textWidths = new float[effectRows.size()];

        for (int rowIndex = 0; rowIndex < effectRows.size(); rowIndex++) {
            EffectRow effectRow = effectRows.get(rowIndex);
            float textWidth;
            if (detailedLayout) {
                float nameWidth = glRenderer.measureMiniMessageTextWidth(effectRow.nameText);
                float durationWidth = glRenderer.measureMiniMessageTextWidth(effectRow.durationText);
                textWidth = Math.max(nameWidth, durationWidth);
            }
            else {
                String compactSegment = compactText(effectRow.nameText, effectRow.durationText, showDurationValue);
                textWidth = glRenderer.measureMiniMessageTextWidth(compactSegment);
            }
            textWidths[rowIndex] = textWidth;
            maxRowWidth = Math.max(maxRowWidth, ICON_SIZE + ICON_TEXT_GAP + textWidth);
        }

        float contentHeight = effectRows.size() * rowHeight + Math.max(0f, effectRows.size() - 1f) * ROW_GAP;
        float padding = hudHostModule().getPadding();
        float layoutWidth = Math.max(1f, Math.max(getMinWidth(), padding * 2f + maxRowWidth));
        float layoutHeight = Math.max(1f, padding * 2f + contentHeight);

        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);

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
                float durationY = textBlockY + fontHeight + TEXT_LINE_GAP;

                float flashAlpha = effectRow.flashAlpha;
                if (rightAligned) {
                    if (detailedLayout) {
                        float nameX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(effectRow.nameText);
                        float durationX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(effectRow.durationText);
                        glRenderer.drawMiniMessageText(effectRow.nameText, nameX, textBlockY, textShadow);
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(effectRow.durationText, durationX, durationY, textShadow);
                        glRenderer.resetMultiplyAlpha();
                    }
                    else {
                        String compactLine = compactText(effectRow.nameText, effectRow.durationText, showDurationValue);
                        float compactX = textRightEdgeWhenRight - glRenderer.measureMiniMessageTextWidth(compactLine);
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(compactLine, compactX, textBlockY, textShadow);
                        glRenderer.resetMultiplyAlpha();
                    }
                    glRenderer.drawSprite(effectRow.effectSprite, sharedIconXWhenRight, iconY, ICON_SIZE, ICON_SIZE, whiteWithAlpha(1f));
                }
                else {
                    float rowWidth = ICON_SIZE + ICON_TEXT_GAP + textWidths[rowIndex];
                    float rowStartX = padding + HudAnchorLayout.horizontalOffsetInInnerBand(innerWidth, rowWidth, HudAnchorContentAlignment.Horizontal.LEFT);
                    float textX = rowStartX + ICON_SIZE + ICON_TEXT_GAP;
                    glRenderer.drawSprite(effectRow.effectSprite, rowStartX, iconY, ICON_SIZE, ICON_SIZE, whiteWithAlpha(1f));
                    if (detailedLayout) {
                        glRenderer.drawMiniMessageText(effectRow.nameText, textX, textBlockY, textShadow);
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(effectRow.durationText, textX, durationY, textShadow);
                        glRenderer.resetMultiplyAlpha();
                    }
                    else {
                        glRenderer.setMultiplyAlpha(flashAlpha);
                        glRenderer.drawMiniMessageText(compactText(effectRow.nameText, effectRow.durationText, showDurationValue), textX, textBlockY, textShadow);
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

    private static int whiteWithAlpha(float alpha) {
        int alphaChannel = Mth.clamp(Math.round(alpha * 255f), 0, 255);
        return (alphaChannel << 24) | 0x00FFFFFF;
    }

    private static Identifier spriteForMobEffect(Holder<MobEffect> effect) {
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

    private float durationFlashAlpha(MobEffectInstance effectInstance) {
        if (effectInstance.isAmbient()) {
            return 1f;
        }
        int flashWindowTicks = Math.round(statusEffectsModule.effectsFlashEndingSeconds() * 20f);
        if (flashWindowTicks <= 0) {
            return 1f;
        }
        if (!effectInstance.endsWithin(flashWindowTicks)) {
            return 1f;
        }
        int remainingSeconds = effectInstance.getDuration() / 20;
        return (remainingSeconds % 2 == 0) ? 1f : 0f;
    }

    private List<EffectRow> resolveEffectRows(boolean editorMode) {
        boolean includeAmplifier = statusEffectsModule.effectsShowAmplifier();

        if (minecraft.player != null) {
            List<MobEffectInstance> activeEffects = new ArrayList<>(minecraft.player.getActiveEffects());
            activeEffects.removeIf(effectInstance -> !effectInstance.showIcon());
            if (!activeEffects.isEmpty()) {
                if (statusEffectsModule.effectsSortMode() == SortMode.ALPHABETICAL) {
                    activeEffects.sort(Comparator.comparing(effectInstance -> effectInstance.getEffect().value().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER));
                }
                else {
                    activeEffects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
                }
                List<EffectRow> rows = new ArrayList<>(activeEffects.size());
                for (MobEffectInstance effectInstance : activeEffects) {
                    String effectName = formatEffectNameWithAmplifier(effectInstance, includeAmplifier);
                    String durationText = effectInstance.isInfiniteDuration()
                            ? Component.translatable("effect.duration.infinite").getString()
                            : DateUtils.formatDuration(effectInstance.getDuration());
                    rows.add(new EffectRow(spriteForMobEffect(effectInstance.getEffect()), effectName, durationText, durationFlashAlpha(effectInstance)));
                }
                return rows;
            }
        }

        if (editorMode) {
            return List.of(new EffectRow(PREVIEW_SPEED_ICON, includeAmplifier ? "Speed II" : "Speed", "01:25", 1f), new EffectRow(PREVIEW_POISON_ICON, includeAmplifier ? "Poison II" : "Poison", "00:37", 0.9f));
        }
        return List.of();
    }

    private record EffectRow(Identifier effectSprite, String nameText, String durationText, float flashAlpha) {}
}
