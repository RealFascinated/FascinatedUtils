package cc.fascinated.fascinatedutils.systems.modules.impl.wawla;

import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.mixin.ClientPlayerInteractionManagerAccessorMixin;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions.*;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.hud.WawlaHudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WawlaWidget extends HudHostModule {

    private static final Map<Block, WawlaBlockExtension<?>> BLOCK_EXTENSIONS = new HashMap<>();
    private static final List<WawlaEntityExtension<?>> ENTITY_EXTENSIONS = new ArrayList<>();
    private static CrosshairTarget editorCrosshairPreview;

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public WawlaWidget() {
        super("wawla", "WAWLA", HudDefaults.builder().build());
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

        registerEntityExtension(new HealthExtension());
        registerHudPanel(new WawlaHudPanel(this));
    }

    /**
     * Registers a block extension that provides extra display data for a specific block.
     *
     * @param extension the extension to register
     */
    public static void registerBlockExtension(WawlaBlockExtension<?> extension) {
        BLOCK_EXTENSIONS.put(extension.getBlock(), extension);
    }

    public static void registerEntityExtension(WawlaEntityExtension<?> extension) {
        ENTITY_EXTENSIONS.add(extension);
    }

    public @Nullable CrosshairTarget resolveCrosshairTarget(boolean editorMode) {
        if (editorMode) {
            if (editorCrosshairPreview == null) {
                editorCrosshairPreview = new CrosshairTarget("Sandstone", null, "Minecraft", new ItemStack(Items.SANDSTONE),
                        0.65f, true, List.of());
            }
            return editorCrosshairPreview;
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
            return new CrosshairTarget(displayName, null, sourceName, iconStack, breakProgress, breakProgress > 0f, subtitleLines);
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
            List<String> entitySubtitleLines = ENTITY_EXTENSIONS.stream()
                    .filter(ext -> ext.matches(entity))
                    .flatMap(ext -> ext.apply(entity).stream())
                    .toList();
            return new CrosshairTarget(displayName, entityName, sourceName, iconStack, 0f, false, entitySubtitleLines);
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

    private static float resolveBreakProgress(Minecraft mc, BlockPos blockPos) {
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

    public record CrosshairTarget(String displayName, String entityName, String sourceName, ItemStack iconStack,
                                  float breakProgress, boolean showBreakBar,
                                  List<String> subtitleLines) {}
}
