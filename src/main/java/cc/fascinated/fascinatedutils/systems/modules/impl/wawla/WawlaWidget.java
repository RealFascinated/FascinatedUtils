package cc.fascinated.fascinatedutils.systems.modules.impl.wawla;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.hooks.FadeInAnim;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.mixin.ClientPlayerInteractionManagerAccessorMixin;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WawlaWidget extends HudModule {

    private static final Map<Block, WawlaBlockExtension<?>> BLOCK_EXTENSIONS = new HashMap<>();
    private static TargetInfo editorItem;

    private static final float
            ICON_SIZE = 16f,
            ICON_TEXT_GAP = 5f,
            LINE_GAP = 1f,
            BREAK_BAR_HEIGHT = 2f,
            BREAK_BAR_LERP_SPEED = 14f;

    private static final int
            TITLE_COLOR = UiColor.argb("#f2f6ff"),
            SOURCE_COLOR = UiColor.argb("#7f91ff"),
            SUBTITLE_COLOR = UiColor.argb("#aaaaaa"),
            BREAK_BAR_BACKGROUND = UiColor.argb("#44232a33"),
            BREAK_BAR_FILL = UiColor.argb("#ffffffff");

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    @Nullable
    private TargetInfo lastTarget;
    private float smoothedBreakProgress;
    private final FadeInAnim fadeAnim = new FadeInAnim(100f);

    /**
     * Registers a block extension that provides extra display data for a specific block.
     *
     * @param extension the extension to register
     */
    public static void registerBlockExtension(WawlaBlockExtension<?> extension) {
        BLOCK_EXTENSIONS.put(extension.getBlock(), extension);
    }

    public WawlaWidget() {
        super("wawla", "WAWLA", 0f);
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

        registerBlockExtension(new CropGrowthExtension((CropBlock) Blocks.WHEAT));
        registerBlockExtension(new CropGrowthExtension((CropBlock) Blocks.CARROTS));
        registerBlockExtension(new CropGrowthExtension((CropBlock) Blocks.POTATOES));
        registerBlockExtension(new CropGrowthExtension((CropBlock) Blocks.BEETROOTS));
        registerBlockExtension(new CropGrowthExtension((CropBlock) Blocks.TORCHFLOWER_CROP));
        registerBlockExtension(new NetherWartExtension((NetherWartBlock) Blocks.NETHER_WART));
        registerBlockExtension(new SweetBerryExtension((SweetBerryBushBlock) Blocks.SWEET_BERRY_BUSH));
        registerBlockExtension(new ComposterExtension((ComposterBlock) Blocks.COMPOSTER));
        registerBlockExtension(new BeehiveExtension((BeehiveBlock) Blocks.BEEHIVE));
        registerBlockExtension(new BeehiveExtension((BeehiveBlock) Blocks.BEE_NEST));
        registerBlockExtension(new RepeaterExtension((RepeaterBlock) Blocks.REPEATER));
        registerBlockExtension(new RedstoneWireExtension((RedStoneWireBlock) Blocks.REDSTONE_WIRE));
        registerBlockExtension(new ComparatorExtension((ComparatorBlock) Blocks.COMPARATOR));
        registerBlockExtension(new DaylightDetectorExtension((DaylightDetectorBlock) Blocks.DAYLIGHT_DETECTOR));
        registerBlockExtension(new TargetExtension((TargetBlock) Blocks.TARGET));
        registerBlockExtension(new SculkSensorExtension((SculkSensorBlock) Blocks.SCULK_SENSOR));
        registerBlockExtension(new SculkSensorExtension((SculkSensorBlock) Blocks.CALIBRATED_SCULK_SENSOR));
        registerBlockExtension(new FarmlandExtension((FarmlandBlock) Blocks.FARMLAND));
    }

    @Override
    @Nullable
    public Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        TargetInfo displayTarget = resolveTarget(editorMode);

        // todo: fix item fading
        fadeAnim.tick(deltaSeconds);
        if (displayTarget != null) {
            lastTarget = displayTarget;
            fadeAnim.show();
        } else {
            fadeAnim.hide();
        }
        if (!fadeAnim.isVisible()) {
            return null;
        }

        TargetInfo target = lastTarget;
        float lineHeight = glRenderer.getFontHeight();
        List<String> subtitleLines = target.subtitleLines();
        int extraLines = subtitleLines.size();
        int totalLines = 2 + extraLines;
        float textBlockHeight = lineHeight * totalLines + LINE_GAP * (totalLines - 1);
        float contentHeight = Math.max(ICON_SIZE, textBlockHeight);

        String rawDisplayName = target.displayName();
        String strippedDisplayName = rawDisplayName == null ? "" : rawDisplayName.replaceAll("§.", "").trim();
        String titleMini = "<color:" + Colors.rgbHex(TITLE_COLOR) + ">" + (strippedDisplayName.isEmpty() ? target.entityName() : rawDisplayName) + "</color>";
        List<String> subtitleMinis = subtitleLines.stream()
                .map(line -> "<color:" + Colors.rgbHex(SUBTITLE_COLOR) + ">" + line + "</color>")
                .toList();
        float[] subtitleWidths = new float[subtitleMinis.size()];
        float subtitleMaxWidth = 0f;
        for (int subtitleIndex = 0; subtitleIndex < subtitleMinis.size(); subtitleIndex++) {
            subtitleWidths[subtitleIndex] = glRenderer.measureMiniMessageTextWidth(subtitleMinis.get(subtitleIndex));
            subtitleMaxWidth = Math.max(subtitleMaxWidth, subtitleWidths[subtitleIndex]);
        }
        String sourceMini = target.showEntityHealth()
                ? "<white>" + target.sourceName() + "</white> <color:#ff5555>❤</color>"
                : "<i><color:" + Colors.rgbHex(SOURCE_COLOR) + ">" + target.sourceName() + "</color></i>";

        float titleWidth = glRenderer.measureMiniMessageTextWidth(titleMini);
        float sourceWidth = glRenderer.measureMiniMessageTextWidth(sourceMini);
        float textBlockWidth = Math.max(Math.max(titleWidth, subtitleMaxWidth), sourceWidth);
        float panelPadding = getPadding();

        if (target.showBreakBar()) {
            smoothedBreakProgress = Mth.lerp(Mth.clamp(deltaSeconds * BREAK_BAR_LERP_SPEED, 0f, 1f), smoothedBreakProgress, Mth.clamp(target.breakProgress(), 0f, 1f));
        } else {
            smoothedBreakProgress = 0f;
        }

        boolean renderBreakBar = target.showBreakBar() && smoothedBreakProgress > 0.001f;
        float layoutWidth = Math.max(getMinWidth(), panelPadding * 2f + ICON_SIZE + ICON_TEXT_GAP + textBlockWidth);
        float layoutHeight = panelPadding * 2f + contentHeight;
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        float fadeAlpha = fadeAnim.progress().value();
        float capturedBreakProgress = smoothedBreakProgress;
        return () -> {
            glRenderer.setMultiplyAlpha(fadeAlpha);
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);
            float iconY = panelPadding + HudAnchorLayout.verticalOffsetInInnerBand(contentHeight, ICON_SIZE, hudContentVerticalAlignment());
            float textBlockY = panelPadding + HudAnchorLayout.verticalOffsetInInnerBand(contentHeight, textBlockHeight, hudContentVerticalAlignment());
            boolean rightAligned = hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            if (!rightAligned) {
                glRenderer.drawGuiItem(target.iconStack(), panelPadding, iconY);
                float textX = panelPadding + ICON_SIZE + ICON_TEXT_GAP;
                glRenderer.drawMiniMessageText(titleMini, textX, textBlockY, false);
                for (int si = 0; si < subtitleMinis.size(); si++) {
                    glRenderer.drawMiniMessageText(subtitleMinis.get(si), textX, textBlockY + (lineHeight + LINE_GAP) * (si + 1), false);
                }
                glRenderer.drawMiniMessageText(sourceMini, textX, textBlockY + (lineHeight + LINE_GAP) * (extraLines + 1), false);
            } else {
                float iconX = layoutWidth - panelPadding - ICON_SIZE;
                glRenderer.drawGuiItem(target.iconStack(), iconX, iconY);
                float textRightEdge = iconX - ICON_TEXT_GAP;
                glRenderer.drawMiniMessageText(titleMini, textRightEdge - titleWidth, textBlockY, false);
                for (int si = 0; si < subtitleMinis.size(); si++) {
                    glRenderer.drawMiniMessageText(subtitleMinis.get(si), textRightEdge - subtitleWidths[si], textBlockY + (lineHeight + LINE_GAP) * (si + 1), false);
                }
                glRenderer.drawMiniMessageText(sourceMini, textRightEdge - sourceWidth, textBlockY + (lineHeight + LINE_GAP) * (extraLines + 1), false);
            }

            if (renderBreakBar) {
                float barY = layoutHeight - BREAK_BAR_HEIGHT;
                float barWidth = layoutWidth - panelPadding * 2f;
                glRenderer.drawRect(panelPadding, barY, barWidth, BREAK_BAR_HEIGHT, BREAK_BAR_BACKGROUND);
                glRenderer.drawRect(panelPadding, barY, barWidth * capturedBreakProgress, BREAK_BAR_HEIGHT, BREAK_BAR_FILL);
            }
            glRenderer.resetMultiplyAlpha();
        };
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Nullable
    private TargetInfo resolveTarget(boolean editorMode) {
        if (editorMode) {
            if (editorItem == null) {
                editorItem = new TargetInfo("Sandstone", null, "Minecraft", new ItemStack(Items.SANDSTONE),
                        false, 0.65f, true, List.of());
            }
            return editorItem;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        HitResult crosshairTarget = mc.hitResult;
        if (crosshairTarget == null || crosshairTarget.getType() == HitResult.Type.MISS) {
            return null;
        }

        if (crosshairTarget.getType() == HitResult.Type.BLOCK && crosshairTarget instanceof BlockHitResult blockHit) {
            BlockPos blockPos = blockHit.getBlockPos();
            var blockState = mc.level.getBlockState(blockPos);
            var block = blockState.getBlock();
            String displayName = block.getName().getString();
            String sourceName = formatSourceName(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
            Item iconItem = block.asItem();
            ItemStack iconStack = iconItem == Items.AIR ? new ItemStack(Items.SANDSTONE) : new ItemStack(iconItem);
            float breakProgress = resolveBreakProgress(mc, blockPos);
            WawlaBlockExtension<?> blockExtension = BLOCK_EXTENSIONS.get(block);
            List<String> subtitleLines = blockExtension != null ? blockExtension.getExtension(blockState) : List.of();
            return new TargetInfo(displayName, null, sourceName, iconStack, false, breakProgress, breakProgress > 0f, subtitleLines);
        }
        if (crosshairTarget.getType() == HitResult.Type.ENTITY && crosshairTarget instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            String displayName = entity.getName().getString();
            String entityName = entity.getType().getDescription().getString();
            String sourceName = formatSourceName(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
            ItemStack iconStack;
            if (entity instanceof Player player) {
                iconStack = new ItemStack(Items.PLAYER_HEAD);
                iconStack.set(DataComponents.PROFILE, player.getProfile());
            } else {
                iconStack = SpawnEggItem.byId(entity.getType()).map(ItemStack::new).orElse(ItemStack.EMPTY);
            }
            boolean showHealth = entity instanceof LivingEntity;
            if (showHealth) {
                sourceName = formatHealthLine((LivingEntity) entity);
            }
            return new TargetInfo(displayName, entityName, sourceName, iconStack, showHealth, 0f, false, List.of());
        }
        return null;
    }

    private static String formatSourceName(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "Minecraft";
        }
        String[] words = namespace.replace('_', ' ').toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(StringUtils.capitalize(words[i]));
        }
        return builder.toString();
    }

    private static String formatHealthLine(LivingEntity entity) {
        return formatNumber(entity.getHealth()) + "/" + formatNumber(entity.getMaxHealth());
    }

    private static String formatNumber(float value) {
        if (!Float.isFinite(value)) {
            return "0";
        }
        float rounded = Math.round(value * 10f) / 10f;
        return Math.abs(rounded - Math.round(rounded)) < 0.001f ? Integer.toString(Math.round(rounded)) : Float.toString(rounded);
    }

    private float resolveBreakProgress(Minecraft mc, BlockPos blockPos) {
        if (mc.gameMode == null || !mc.gameMode.isDestroying()) {
            return 0f;
        }
        ClientPlayerInteractionManagerAccessorMixin accessor = (ClientPlayerInteractionManagerAccessorMixin) mc.gameMode;
        BlockPos currentBreakingPos = accessor.fascinatedutils$getCurrentBreakingPos();
        if (currentBreakingPos == null || !currentBreakingPos.equals(blockPos)) {
            return 0f;
        }
        return Mth.clamp(accessor.fascinatedutils$getCurrentBreakingProgress(), 0f, 1f);
    }

    private record TargetInfo(String displayName, String entityName, String sourceName, ItemStack iconStack,
                              boolean showEntityHealth, float breakProgress, boolean showBreakBar,
                              List<String> subtitleLines) {}
}