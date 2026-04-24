package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.TpsColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorWidget extends HudModule {
    private static final int ROW_COUNT = 6;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String SLOTS_CATEGORY_DISPLAY_KEY = "Slots";

    private final BooleanSetting[] slotRowVisibility = {
            BooleanSetting.builder().id("show_head").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_chest").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_legs").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_feet").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_main_hand").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_off_hand").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build()
    };

    private final BooleanSetting hideUnbreakableDurability = BooleanSetting.builder()
            .id("hide_unbreakable_durability")
            .defaultValue(false)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showTotalInventoryCount = BooleanSetting.builder()
            .id("show_total_item_count")
            .defaultValue(true)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    private final BooleanSetting colorArmorDurability = BooleanSetting.builder()
            .id("color_armor_durability")
            .defaultValue(true)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting colorMainHandDurability = BooleanSetting.builder()
            .id("color_main_hand_durability")
            .defaultValue(true)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting colorOffHandDurability = BooleanSetting.builder()
            .id("color_off_hand_durability")
            .defaultValue(true)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public ArmorWidget() {
        super("armor_hud", "Armor HUD", 0f, HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.BOTTOM_RIGHT).defaultXOffset(5).defaultYOffset(5).build());
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
        for (int slotIndex = 0; slotIndex < ROW_COUNT; slotIndex++) {
            addSetting(slotRowVisibility[slotIndex]);
        }
        addSetting(hideUnbreakableDurability);
        addSetting(showTotalInventoryCount);
        addSetting(colorArmorDurability);
        addSetting(colorMainHandDurability);
        addSetting(colorOffHandDurability);
    }

    private static ItemStack previewStack(Item item, int damage) {
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(damage);
        return stack;
    }

    private static ItemStack editorPreviewStackForRow(int rowIndex) {
        return switch (rowIndex) {
            case 0 -> previewStack(Items.DIAMOND_HELMET, 120);
            case 1 -> previewStack(Items.DIAMOND_CHESTPLATE, 240);
            case 2 -> previewStack(Items.DIAMOND_LEGGINGS, 180);
            case 3 -> previewStack(Items.DIAMOND_BOOTS, 90);
            case 4 -> previewStack(Items.DIAMOND_SWORD, 200);
            case 5 -> previewStack(Items.SHIELD, 150);
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack stackForRow(Player player, int rowIndex) {
        if (rowIndex < ARMOR_SLOTS.length) {
            return player.getItemBySlot(ARMOR_SLOTS[rowIndex]);
        }
        if (rowIndex == ARMOR_SLOTS.length) {
            return player.getMainHandItem();
        }
        if (rowIndex == ARMOR_SLOTS.length + 1) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rows = new ArrayList<>(ROW_COUNT);
        Player player = Minecraft.getInstance().player;

        if (editorMode && !hasAnyVisibleEquippedStack(player)) {
            for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
                if (!isSlotRowShown(rowIndex)) {
                    continue;
                }
                ItemStack stack = editorPreviewStackForRow(rowIndex);
                if (stack.isEmpty()) {
                    continue;
                }
                rows.add(new HudContent.ItemRow(stack, durabilityOnlyText(stack, shouldColorRow(rowIndex))));
            }
        }
        else {
            if (player == null || !hasAnyVisibleEquippedStack(player)) {
                return null;
            }
            for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
                if (!isSlotRowShown(rowIndex)) {
                    continue;
                }
                ItemStack stack = stackForRow(player, rowIndex);
                if (stack.isEmpty()) {
                    continue;
                }
                rows.add(new HudContent.ItemRow(resolvedRowStack(stack, player), durabilityOnlyText(stack, shouldColorRow(rowIndex))));
            }
        }
        return rows.isEmpty() ? null : new HudContent.ItemRows(rows);
    }

    private ItemStack resolvedRowStack(ItemStack stack, Player player) {
        if (showTotalInventoryCount.isEnabled() && stack.getMaxDamage() <= 0 && stack.getMaxStackSize() > 1) {
            int count = 0;
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack slotStack = inventory.getItem(slot);
                if (!slotStack.isEmpty() && slotStack.getItem() == stack.getItem()) {
                    count += slotStack.getCount();
                }
            }
            if (count > 0) {
                return stack.copyWithCount(count);
            }
        }
        return stack;
    }

    private boolean shouldColorRow(int rowIndex) {
        if (rowIndex < ARMOR_SLOTS.length) {
            return colorArmorDurability.isEnabled();
        }
        if (rowIndex == ARMOR_SLOTS.length) {
            return colorMainHandDurability.isEnabled();
        }
        return colorOffHandDurability.isEnabled();
    }

    private String durabilityOnlyText(ItemStack stack, boolean useColor) {
        if (stack.isEmpty() || stack.getMaxDamage() <= 0) {
            return "";
        }
        if (hideUnbreakableDurability.isEnabled() && stack.has(DataComponents.UNBREAKABLE)) {
            return "";
        }
        int durability = stack.getMaxDamage() - stack.getDamageValue();
        if (useColor) {
            float percent = (float) durability / stack.getMaxDamage();
            String colorHex = Colors.rgbHex(TpsColors.getTpsColor(percent * 20f));
            return "<color:" + colorHex + ">" + durability + "</color>";
        }
        return "<white>" + durability + "</white>";
    }

    private boolean isSlotRowShown(int rowIndex) {
        return rowIndex >= 0 && rowIndex < ROW_COUNT && slotRowVisibility[rowIndex].isEnabled();
    }

    private boolean hasAnyVisibleEquippedStack(Player player) {
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            if (isSlotRowShown(rowIndex) && !stackForRow(player, rowIndex).isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
