package cc.fascinated.fascinatedutils.systems.modules.impl.armor.hud;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.common.color.TpsColors;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.RichTextNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.ItemStackNode;
import cc.fascinated.fascinatedutils.gui.node.layout.RowNode;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudComposeContext;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.modules.impl.armor.ArmorModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorHudPanel extends HudPanel {

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int OFF_HAND_ROW_INDEX = ARMOR_SLOTS.length;
    private static final int MAIN_HAND_ROW_INDEX = ARMOR_SLOTS.length + 1;
    private static final int ICON_GAP_PX = 2;
    private static final int ICON_TEXT_GAP_PX = 4;

    private final ArmorModule armorModule;
    private final Map<Item, Integer> inventoryTotals = new HashMap<>();
    private int inventoryTotalsTick = -1;
    private Player inventoryTotalsPlayer;

    public ArmorHudPanel(ArmorModule armorModule) {
        super(armorModule, "armor_hud", 0f);
        this.armorModule = armorModule;
    }

    private static ItemStack editorPreviewStackForRow(int rowIndex) {
        return switch (rowIndex) {
            case 0 -> ItemStackNode.previewStack(Items.DIAMOND_HELMET);
            case 1 -> ItemStackNode.previewStack(Items.DIAMOND_CHESTPLATE);
            case 2 -> ItemStackNode.previewStack(Items.DIAMOND_LEGGINGS);
            case 3 -> ItemStackNode.previewStack(Items.DIAMOND_BOOTS);
            case 4 -> ItemStackNode.previewStack(Items.SHIELD);
            case 5 -> ItemStackNode.previewStack(Items.DIAMOND_SWORD);
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

    @Override
    protected @Nullable UiNode compose(HudComposeContext context) {
        Player player = Minecraft.getInstance().player;
        boolean hasVisibleEquippedStack = player != null && hasAnyVisibleEquippedStack(player);
        boolean showEditorPreview = context.editorMode() && !hasVisibleEquippedStack;

        if (!showEditorPreview && !hasVisibleEquippedStack) {
            return null;
        }

        boolean alignRight = context.contentAlignment() == 1f;
        float alignment = alignRight ? 1f : 0f;
        ColumnNode column = new ColumnNode(Math.round(HudAnchorLayout.LINE_GAP_PX)).alignX(alignment);
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
                column.addChild(itemRow(context, alignRight, List.of(offHandDisplayStack, displayStack), combinedHandDurabilityText(offHandSourceStack, sourceStack)));
                continue;
            }

            column.addChild(itemRow(context, alignRight, List.of(displayStack), durabilityOnlyText(sourceStack, shouldColorRow(rowIndex))));
        }
        return column.childrenView().isEmpty() ? null : column;
    }

    /**
     * One row: the icons side by side and the durability text, icons first unless the panel hangs from the right.
     */
    private static RowNode itemRow(HudComposeContext context, boolean alignRight, List<ItemStack> stacks, String text) {
        RowNode icons = new RowNode(ICON_GAP_PX).alignY(0.5f);
        for (ItemStack stack : stacks) {
            icons.addChild(new ItemStackNode(stack));
        }
        // Blank text must not reserve the gap between icon and text.
        RichTextNode textNode = text.isBlank() ? null : context.text(text).alignY(0.5f);
        RowNode row = new RowNode(0).alignX(alignRight ? 1f : 0f);
        if (alignRight) {
            row.addChild(textNode);
            row.addChild(textNode == null ? icons : icons.left(ICON_TEXT_GAP_PX));
        }
        else {
            row.addChild(icons);
            row.addChild(textNode == null ? null : textNode.left(ICON_TEXT_GAP_PX));
        }
        return row;
    }

    private ItemStack resolvedRowStack(ItemStack stack, Player player) {
        if (armorModule.armorHudShowTotalInventoryCount() && stack.getMaxDamage() <= 0 && stack.getMaxStackSize() > 1) {
            int count = inventoryTotal(player, stack.getItem());
            if (count > 0) {
                return stack.copyWithCount(count);
            }
        }
        return stack;
    }

    private int inventoryTotal(Player player, Item item) {
        if (inventoryTotalsPlayer != player || inventoryTotalsTick != player.tickCount) {
            inventoryTotals.clear();
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack slotStack = inventory.getItem(slot);
                if (!slotStack.isEmpty()) {
                    inventoryTotals.merge(slotStack.getItem(), slotStack.getCount(), Integer::sum);
                }
            }
            inventoryTotalsPlayer = player;
            inventoryTotalsTick = player.tickCount;
        }
        return inventoryTotals.getOrDefault(item, 0);
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
        if (stack.isEmpty() || stack.getMaxDamage() <= 0) {
            return "";
        }
        if (armorModule.armorHudHideUnbreakableDurability() && stack.has(DataComponents.UNBREAKABLE)) {
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
