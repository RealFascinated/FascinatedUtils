package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.mixin.ClientPlayerInteractionManagerAccessorMixin;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public class WawlaWidget extends HudModule {
    private static final float PANEL_PADDING_X = 5f, PANEL_PADDING_Y = 4f, ICON_SIZE = 16f, ICON_TEXT_GAP = 5f, LINE_GAP = 1f, BREAK_BAR_GAP = 3f, BREAK_BAR_HEIGHT = 2f, BREAK_BAR_LERP_SPEED = 14f;
    private static final int TITLE_COLOR = UiColor.argb("#f2f6ff"), SOURCE_COLOR = UiColor.argb("#7f91ff"), BREAK_BAR_BACKGROUND = UiColor.argb("#44232a33"), BREAK_BAR_FILL = UiColor.argb("#ffffffff");
    private final BooleanSetting showBackground = BooleanSetting.builder().id(SETTING_SHOW_BACKGROUND).defaultValue(true).translationKeyPath("fascinatedutils.module.show_hud_background").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBorder = BooleanSetting.builder().id(SETTING_SHOW_BORDER).defaultValue(false).translationKeyPath("fascinatedutils.module.show_border").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting borderThickness = SliderSetting.builder().id(SETTING_BORDER_THICKNESS).defaultValue(2f).minValue(1f).maxValue(3f).step(1f).translationKeyPath("fascinatedutils.module.border_thickness").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting padding = SliderSetting.builder().id(SETTING_PADDING).defaultValue(6f).minValue(0f).maxValue(16f).step(1f).translationKeyPath("fascinatedutils.module.padding").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    @Nullable
    private BlockPos activeBreakingPos;
    private float smoothedBreakProgress;

    public WawlaWidget() {
        super("wawla", "WAWLA", 0f);
        addSetting(showBackground);
        addSetting(showBorder);
        addSetting(borderThickness);
        addSetting(padding);
    }

    private static String formatSourceName(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "Minecraft";
        }
        String[] words = namespace.replace('_', ' ').toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            if (wordIndex > 0) {
                builder.append(' ');
            }
            builder.append(StringUtils.capitalize(words[wordIndex]));
        }
        return builder.toString();
    }

    private static String formatHealthLine(LivingEntity livingEntity) {
        return formatNumber(livingEntity.getHealth()) + "/" + formatNumber(livingEntity.getMaxHealth());
    }

    private static String formatNumber(float value) {
        if (!Float.isFinite(value)) {
            return "0";
        }
        float rounded = Math.round(value * 10f) / 10f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.001f) {
            return Integer.toString(Math.round(rounded));
        }
        return Float.toString(rounded);
    }

    @Override
    @Nullable
    public Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        TargetInfo targetInfo = resolveTarget(editorMode);
        if (targetInfo == null) {
            return null;
        }
        float lineHeight = glRenderer.getFontHeight();
        float textBlockHeight = lineHeight * 2f + LINE_GAP;
        float contentHeight = Math.max(ICON_SIZE, textBlockHeight);
        String titleMini = "<b><color:" + ColorUtils.rgbHex(TITLE_COLOR) + ">" + targetInfo.displayName() + "</color></b>";
        String secondaryMini = targetInfo.showEntityHealth() ? "<white>" + targetInfo.sourceName() + "</white> <color:#ff5555>\u2764</color>" : "<color:" + ColorUtils.rgbHex(SOURCE_COLOR) + ">" + targetInfo.sourceName() + "</color>";
        float line1Width = glRenderer.measureMiniMessageTextWidth(titleMini);
        float line2Width = glRenderer.measureMiniMessageTextWidth(secondaryMini);
        float textWidth = Math.max(line1Width, line2Width);
        float layoutWidth = Math.max(getMinWidth(), PANEL_PADDING_X * 2f + ICON_SIZE + ICON_TEXT_GAP + textWidth);
        float targetBreakProgress = Mth.clamp(targetInfo.breakProgress(), 0f, 1f);
        if (targetInfo.showBreakBar()) {
            smoothedBreakProgress = Mth.lerp(Mth.clamp(deltaSeconds * BREAK_BAR_LERP_SPEED, 0f, 1f), smoothedBreakProgress, targetBreakProgress);
        }
        else {
            smoothedBreakProgress = 0f;
        }
        boolean renderBreakBar = targetInfo.showBreakBar() && smoothedBreakProgress > 0.001f;
        float layoutHeight = PANEL_PADDING_Y * 2f + contentHeight + (renderBreakBar ? BREAK_BAR_GAP + BREAK_BAR_HEIGHT : 0f);
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        float capturedSmoothedBreakProgress = smoothedBreakProgress;
        return () -> {
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);
            float innerTop = PANEL_PADDING_Y;
            float iconY = innerTop + HudAnchorLayout.verticalOffsetInInnerBand(contentHeight, ICON_SIZE, hudContentVerticalAlignment());
            float textY = innerTop + HudAnchorLayout.verticalOffsetInInnerBand(contentHeight, textBlockHeight, hudContentVerticalAlignment());
            boolean mirrorIconAndText = hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;
            if (!mirrorIconAndText) {
                float iconX = PANEL_PADDING_X;
                glRenderer.drawGuiItem(targetInfo.iconStack(), iconX, iconY);
                float textX = iconX + ICON_SIZE + ICON_TEXT_GAP;
                glRenderer.drawMiniMessageText(titleMini, textX, textY, false);
                glRenderer.drawMiniMessageText(secondaryMini, textX, textY + lineHeight + LINE_GAP, false);
            }
            else {
                float iconX = layoutWidth - PANEL_PADDING_X - ICON_SIZE;
                glRenderer.drawGuiItem(targetInfo.iconStack(), iconX, iconY);
                float textRight = iconX - ICON_TEXT_GAP;
                glRenderer.drawMiniMessageText(titleMini, textRight - line1Width, textY, false);
                glRenderer.drawMiniMessageText(secondaryMini, textRight - line2Width, textY + lineHeight + LINE_GAP, false);
            }
            if (renderBreakBar) {
                float barY = PANEL_PADDING_Y + contentHeight + BREAK_BAR_GAP;
                float barWidth = layoutWidth - PANEL_PADDING_X * 2f;
                glRenderer.drawRect(PANEL_PADDING_X, barY, barWidth, BREAK_BAR_HEIGHT, BREAK_BAR_BACKGROUND);
                glRenderer.drawRect(PANEL_PADDING_X, barY, barWidth * capturedSmoothedBreakProgress, BREAK_BAR_HEIGHT, BREAK_BAR_FILL);
            }
        };
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Nullable
    private TargetInfo resolveTarget(boolean editorMode) {
        if (editorMode) {
            return new TargetInfo("Sandstone", "Minecraft", new ItemStack(Items.SANDSTONE), false, 0.65f, true);
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.player == null || minecraftClient.level == null) {
            return null;
        }
        HitResult crosshairTarget = minecraftClient.hitResult;
        if (crosshairTarget == null || crosshairTarget.getType() == HitResult.Type.MISS) {
            return null;
        }
        if (crosshairTarget.getType() == HitResult.Type.BLOCK && crosshairTarget instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            var blockState = minecraftClient.level.getBlockState(blockPos);
            var block = blockState.getBlock();
            String displayName = block.getName().getString();
            String sourceName = formatSourceName(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
            Item iconItem = block.asItem();
            ItemStack iconStack = iconItem == Items.AIR ? new ItemStack(Items.SANDSTONE) : new ItemStack(iconItem);
            BreakingProgress breakingProgress = resolveBreakingProgress(minecraftClient, blockPos);
            return new TargetInfo(displayName, sourceName, iconStack, false, breakingProgress.progress(), breakingProgress.active());
        }
        if (crosshairTarget.getType() == HitResult.Type.ENTITY && crosshairTarget instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            String displayName = entity.getName().getString();
            String sourceName = formatSourceName(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
            ItemStack iconStack = SpawnEggItem.byId(entity.getType()).map(ItemStack::new).orElse(ItemStack.EMPTY);
            boolean showEntityHealth = false;
            if (entity instanceof LivingEntity livingEntity) {
                sourceName = formatHealthLine(livingEntity);
                showEntityHealth = true;
            }
            return new TargetInfo(displayName, sourceName, iconStack, showEntityHealth, 0f, false);
        }
        return null;
    }

    private BreakingProgress resolveBreakingProgress(Minecraft minecraftClient, BlockPos blockPos) {
        if (minecraftClient.gameMode == null || minecraftClient.player == null || minecraftClient.level == null) {
            resetBreakingProgress();
            return new BreakingProgress(0f, false);
        }
        MultiPlayerGameMode interactionManager = minecraftClient.gameMode;
        if (!interactionManager.isDestroying()) {
            resetBreakingProgress();
            return new BreakingProgress(0f, false);
        }
        ClientPlayerInteractionManagerAccessorMixin accessor = (ClientPlayerInteractionManagerAccessorMixin) interactionManager;
        BlockPos currentBreakingPos = accessor.fascinatedutils$getCurrentBreakingPos();
        if (currentBreakingPos == null || !currentBreakingPos.equals(blockPos)) {
            resetBreakingProgress();
            return new BreakingProgress(0f, false);
        }
        if (activeBreakingPos == null || !activeBreakingPos.equals(currentBreakingPos)) {
            activeBreakingPos = currentBreakingPos;
            smoothedBreakProgress = 0f;
        }
        return new BreakingProgress(Mth.clamp(accessor.fascinatedutils$getCurrentBreakingProgress(), 0f, 1f), true);
    }

    private void resetBreakingProgress() {
        activeBreakingPos = null;
        smoothedBreakProgress = 0f;
    }

    private record TargetInfo(String displayName, String sourceName, ItemStack iconStack, boolean showEntityHealth,
                              float breakProgress, boolean showBreakBar) {}

    private record BreakingProgress(float progress, boolean active) {}
}
