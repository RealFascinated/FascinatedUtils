package cc.fascinated.fascinatedutils.systems.modules.impl.armor.hud;

import cc.fascinated.fascinatedutils.caches.ItemStackSizeCache;
import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.TpsColors;
import cc.fascinated.fascinatedutils.systems.modules.impl.armor.ArmorModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArmorHudPanel extends HudPanel {

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int OFF_HAND_ROW_INDEX = ARMOR_SLOTS.length;
    private static final int MAIN_HAND_ROW_INDEX = ARMOR_SLOTS.length + 1;

    private final ArmorModule armorModule;

    public ArmorHudPanel(ArmorModule armorModule) {
        super(armorModule, "armor_hud", 0f);
        this.armorModule = armorModule;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rows = new ArrayList<>(ArmorModule.ARMOR_HUD_ROW_COUNT);
        Player player = Minecraft.getInstance().player;
        boolean hasVisibleEquippedStack = player != null && hasAnyVisibleEquippedStack(player);
        boolean showEditorPreview = editorMode && !hasVisibleEquippedStack;

        if (!showEditorPreview && !hasVisibleEquippedStack) {
            return null;
        }

        boolean combineHandRows = shouldCombineHandRows(player, showEditorPreview);
        for (int rowIndex = 0; rowIndex < ArmorModule.ARMOR_HUD_ROW_COUNT; rowIndex++) {
            if (!isSlotRowShown(rowIndex) || combineHandRows && rowIndex == OFF_HAND_ROW_INDEX) {
                continue;
            }

            ItemStack sourceStack = contentStackForRow(player, showEditorPreview, rowIndex);
            if (sourceStack.isEmpty()) {
                continue;
            }

            ItemStack displayStack = showEditorPreview ? sourceStack : resolvedRowStack(sourceStack, player);
            if (combineHandRows && rowIndex == MAIN_HAND_ROW_INDEX) {
                ItemStack offHandSourceStack = contentStackForRow(player, showEditorPreview, OFF_HAND_ROW_INDEX);
                ItemStack offHandDisplayStack = showEditorPreview ? offHandSourceStack : resolvedRowStack(offHandSourceStack, player);
                rows.add(new HudContent.ItemRow(List.of(offHandDisplayStack), displayStack, combinedHandDurabilityText(offHandSourceStack, sourceStack)));
                continue;
            }

            rows.add(new HudContent.ItemRow(displayStack, durabilityOnlyText(sourceStack, shouldColorRow(rowIndex))));
        }
        return rows.isEmpty() ? null : new HudContent.ItemRows(rows);
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
            case 4 -> previewStack(Items.SHIELD, 150);
            case 5 -> previewStack(Items.DIAMOND_SWORD, 200);
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack stackForRow(Player player, int rowIndex) {
        if (rowIndex < ARMOR_SLOTS.length) {
            return player.getItemBySlot(ARMOR_SLOTS[rowIndex]);
        }
        if (rowIndex == OFF_HAND_ROW_INDEX) {
            return player.getOffhandItem();
        }
        if (rowIndex == MAIN_HAND_ROW_INDEX) {
            return player.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }

    private ItemStack resolvedRowStack(ItemStack stack, Player player) {
        if (armorModule.armorHudShowTotalInventoryCount() && stack.getMaxDamage() <= 0 && stack.getMaxStackSize() > 1) {
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
            return armorModule.armorHudColorArmorDurability();
        }
        if (rowIndex == OFF_HAND_ROW_INDEX) {
            return armorModule.armorHudColorOffHandDurability();
        }
        return armorModule.armorHudColorMainHandDurability();
    }

    private String combinedHandDurabilityText(ItemStack offHandStack, ItemStack mainHandStack) {
        String offHandText = durabilityOnlyText(offHandStack, armorModule.armorHudColorOffHandDurability());
        String mainHandText = durabilityOnlyText(mainHandStack, armorModule.armorHudColorMainHandDurability());
        if (offHandText.isBlank()) {
            return mainHandText;
        }
        if (mainHandText.isBlank()) {
            return offHandText;
        }
        return offHandText + " <white>|</white> " + mainHandText;
    }

    private ItemStack contentStackForRow(Player player, boolean editorPreview, int rowIndex) {
        return editorPreview ? editorPreviewStackForRow(rowIndex) : stackForRow(player, rowIndex);
    }

    private boolean shouldCombineHandRows(Player player, boolean editorPreview) {
        if (!armorModule.armorHudShowOffHandBesideMainHand() || !isSlotRowShown(OFF_HAND_ROW_INDEX) || !isSlotRowShown(MAIN_HAND_ROW_INDEX)) {
            return false;
        }
        return !contentStackForRow(player, editorPreview, OFF_HAND_ROW_INDEX).isEmpty() && !contentStackForRow(player, editorPreview, MAIN_HAND_ROW_INDEX).isEmpty();
    }

    private String durabilityOnlyText(ItemStack stack, boolean useColor) {
        if (stack.isEmpty()) {
            return sizeText(stack);
        }
        String sizeAppend = sizeText(stack);
        if (stack.getMaxDamage() <= 0) {
            return sizeAppend;
        }
        if (armorModule.armorHudHideUnbreakableDurability() && stack.has(DataComponents.UNBREAKABLE)) {
            return sizeAppend;
        }
        int durability = stack.getMaxDamage() - stack.getDamageValue();
        String durabilityText;
        if (useColor) {
            float percent = (float) durability / stack.getMaxDamage();
            String colorHex = Colors.rgbHex(TpsColors.getTpsColor(percent * 20f));
            durabilityText = "<color:" + colorHex + ">" + durability + "</color>";
        } else {
            durabilityText = "<white>" + durability + "</white>";
        }
        return sizeAppend.isBlank() ? durabilityText : durabilityText + " " + sizeAppend;
    }

    private String sizeText(ItemStack stack) {
        if (!armorModule.armorHudShowItemStackSizeBytes() || stack.isEmpty()) {
            return "";
        }
        long bytes = ItemStackSizeCache.getStackSize(stack);
        if (bytes <= 0) {
            return "";
        }
        return "<color:#AAAAAA>" + ByteFormatterUtil.formatBytes(bytes, 2) + "</color>";
    }

    private boolean isSlotRowShown(int rowIndex) {
        return armorModule.armorHudSlotRowEnabled(rowIndex);
    }

    private boolean hasAnyVisibleEquippedStack(Player player) {
        for (int rowIndex = 0; rowIndex < ArmorModule.ARMOR_HUD_ROW_COUNT; rowIndex++) {
            if (isSlotRowShown(rowIndex) && !stackForRow(player, rowIndex).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
