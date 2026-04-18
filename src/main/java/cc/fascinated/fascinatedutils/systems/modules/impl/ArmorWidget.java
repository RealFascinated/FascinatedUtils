package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
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
    private final BooleanSetting[] slotRowVisibility = {BooleanSetting.builder().id("show_head").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(), BooleanSetting.builder().id("show_chest").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(), BooleanSetting.builder().id("show_legs").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(), BooleanSetting.builder().id("show_feet").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(), BooleanSetting.builder().id("show_main_hand").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(), BooleanSetting.builder().id("show_off_hand").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build()};

    public ArmorWidget() {
        super("armor_hud", "Armor HUD", 0f);
        for (int slotIndex = 0; slotIndex < ROW_COUNT; slotIndex++) {
            addSetting(slotRowVisibility[slotIndex]);
        }
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

    private static boolean rowShowsDurabilityNumber(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem();
    }

    private static String durabilityOnlyText(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return "";
        }
        return "<white>" + (stack.getMaxDamage() - stack.getDamageValue()) + "</white>";
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rows = new ArrayList<>(ROW_COUNT);
        if (editorMode) {
            for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
                if (!isSlotRowShown(rowIndex)) {
                    continue;
                }
                ItemStack stack = editorPreviewStackForRow(rowIndex);
                if (stack.isEmpty()) {
                    continue;
                }
                rows.add(new HudContent.ItemRow(stack, durabilityOnlyText(stack)));
            }
        }
        else {
            Player player = Minecraft.getInstance().player;
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
                rows.add(new HudContent.ItemRow(stack, durabilityOnlyText(stack)));
            }
        }
        return rows.isEmpty() ? null : new HudContent.ItemRows(rows);
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
