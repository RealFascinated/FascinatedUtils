package cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.hud;

import cc.fascinated.fascinatedutils.common.util.DateUtils;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.AlphaNode;
import cc.fascinated.fascinatedutils.gui.node.display.RichTextNode;
import cc.fascinated.fascinatedutils.gui.node.display.SpriteNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.gui.node.layout.RowNode;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudComposeContext;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule.DisplayMode;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatusEffectsHudPanel extends HudPanel {

    private static final int ICON_SIZE = 18;
    private static final int ICON_TEXT_GAP = 5;
    private static final int TEXT_LINE_GAP = 1;
    private static final int ROW_GAP = 2;
    private static final Identifier PREVIEW_SPEED_ICON = Identifier.withDefaultNamespace("mob_effect/speed");
    private static final Identifier PREVIEW_POISON_ICON = Identifier.withDefaultNamespace("mob_effect/poison");

    private final StatusEffectsModule statusEffectsModule;
    private final Minecraft minecraft = Minecraft.getInstance();


    public StatusEffectsHudPanel(StatusEffectsModule statusEffectsModule) {
        super(statusEffectsModule, "status_effects", 56f);
        this.statusEffectsModule = statusEffectsModule;
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

    @Override
    protected @Nullable UiNode compose(HudComposeContext context) {
        boolean includeDuration = statusEffectsModule.effectsShowDurationSetting();
        List<EffectRow> rows = resolveEffectRows(context.editorMode());
        if (rows.isEmpty()) {
            return null;
        }
        boolean detailedLayout = statusEffectsModule.effectsDisplayMode() == DisplayMode.DETAILED && includeDuration;
        float alignment = context.contentAlignment() == 1f ? 1f : 0f;
        ColumnNode column = new ColumnNode(ROW_GAP).alignX(alignment);
        for (EffectRow effectRow : rows) {
            column.addChild(effectRowNode(context, effectRow, detailedLayout, includeDuration, alignment));
        }
        return column;
    }

    /**
     * One effect: the sprite beside its text, sprite first unless the panel hangs from the right. The duration fades
     * out while the effect is about to end.
     */
    private RowNode effectRowNode(HudComposeContext context, EffectRow effectRow, boolean detailedLayout, boolean includeDuration, float alignment) {
        String nameText = escapeMiniMessage(effectRow.name());
        String durationText = escapeMiniMessage(effectRow.duration());
        PositionedNode<?> textBlock;
        if (detailedLayout) {
            ColumnNode lines = new ColumnNode(TEXT_LINE_GAP);
            lines.addChild(context.text(nameText).alignX(alignment));
            lines.addChild(fading(effectRow.flashAlpha(), context.text(durationText)).alignX(alignment));
            textBlock = lines.alignY(0.5f);
        }
        else {
            textBlock = fading(effectRow.flashAlpha(), context.text(compactText(nameText, durationText, includeDuration))).alignY(0.5f);
        }
        SpriteNode icon = new SpriteNode(effectRow.sprite()).size(ICON_SIZE).alignY(0.5f);
        RowNode row = new RowNode(0).alignX(alignment);
        if (alignment == 1f) {
            row.addChild(textBlock);
            row.addChild(icon.left(ICON_TEXT_GAP));
        }
        else {
            row.addChild(icon);
            row.addChild(textBlock.left(ICON_TEXT_GAP));
        }
        return row;
    }

    private static AlphaNode fading(float alpha, RichTextNode text) {
        return new AlphaNode(() -> alpha).columnGap(0).addChild(text);
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
            if (statusEffectsModule.effectsSortMode() == SortMode.ALPHABETICAL) {
                activeEffects.sort(Comparator.comparing(effectInstance -> effectInstance.getEffect().value().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER));
            }
            else {
                activeEffects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
            }
            List<EffectRow> newRows = new ArrayList<>(activeEffects.size());
            for (MobEffectInstance effectInstance : activeEffects) {
                String effectName = formatEffectNameWithAmplifier(effectInstance, includeAmplifier);
                String durationText = effectInstance.isInfiniteDuration() ? Component.translatable("effect.duration.infinite").getString() : DateUtils.formatDuration(effectInstance.getDuration());
                newRows.add(new EffectRow(spriteForMobEffect(effectInstance.getEffect()), effectName, durationText, durationFlashAlpha(effectInstance)));
            }
            if (!newRows.isEmpty()) {
                return newRows;
            }
        }

        if (editorMode) {
            return previewRows(includeAmplifier);
        }
        return List.of();
    }

    private List<EffectRow> previewRows(boolean includeAmplifier) {
        return List.of(
                new EffectRow(PREVIEW_SPEED_ICON, hudText(includeAmplifier ? "preview_speed_amplified" : "preview_speed"), "01:25", 1f),
                new EffectRow(PREVIEW_POISON_ICON, hudText(includeAmplifier ? "preview_poison_amplified" : "preview_poison"), "00:37", 0.9f));
    }

    private record EffectRow(Identifier sprite, String name, String duration, float flashAlpha) {}
}
