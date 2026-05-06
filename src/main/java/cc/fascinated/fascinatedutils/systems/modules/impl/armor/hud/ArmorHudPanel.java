package cc.fascinated.fascinatedutils.systems.modules.impl.armor.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.TpsColors;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent.HudItemSpec;
import cc.fascinated.fascinatedutils.systems.modules.impl.armor.ArmorModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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

    private static HudItemSpec editorPreviewSpecForRow(int rowIndex) {
        return switch (rowIndex) {
            case 0 -> new HudItemSpec.Preview(Items.DIAMOND_HELMET);
            case 1 -> new HudItemSpec.Preview(Items.DIAMOND_CHESTPLATE);
            case 2 -> new HudItemSpec.Preview(Items.DIAMOND_LEGGINGS);
            case 3 -> new HudItemSpec.Preview(Items.DIAMOND_BOOTS);
            case 4 -> new HudItemSpec.Preview(Items.SHIELD);
            case 5 -> new HudItemSpec.Preview(Items.DIAMOND_SWORD);
            default -> new HudItemSpec.Real(ItemStack.EMPTY);
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

            HudItemSpec sourceSpec = contentSpecForRow(player, showEditorPreview, rowIndex);
            if (sourceSpec.isEmpty()) {
                continue;
            }

            HudItemSpec displaySpec = showEditorPreview ? sourceSpec : resolvedRowSpec(sourceSpec, player);
            if (combineHandRows && rowIndex == MAIN_HAND_ROW_INDEX) {
                HudItemSpec offHandSourceSpec = contentSpecForRow(player, showEditorPreview, OFF_HAND_ROW_INDEX);
                HudItemSpec offHandDisplaySpec = showEditorPreview ? offHandSourceSpec : resolvedRowSpec(offHandSourceSpec, player);
                rows.add(new HudContent.ItemRow(List.of(offHandDisplaySpec), displaySpec, combinedHandDurabilityText(offHandSourceSpec.toStack(), sourceSpec.toStack())));
                continue;
            }

            rows.add(new HudContent.ItemRow(displaySpec, durabilityOnlyText(sourceSpec.toStack(), shouldColorRow(rowIndex))));
        }
        return rows.isEmpty() ? null : new HudContent.ItemRows(rows);
    }

    private HudItemSpec resolvedRowSpec(HudItemSpec spec, Player player) {
        if (!(spec instanceof HudItemSpec.Real real)) {
            return spec;
        }
        ItemStack stack = real.stack();
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
                return new HudItemSpec.Real(stack.copyWithCount(count));
            }
        }
        return spec;
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

    private HudItemSpec contentSpecForRow(Player player, boolean editorPreview, int rowIndex) {
        return editorPreview ? editorPreviewSpecForRow(rowIndex) : new HudItemSpec.Real(stackForRow(player, rowIndex));
    }

    private boolean shouldCombineHandRows(Player player, boolean editorPreview) {
        if (!armorModule.armorHudShowOffHandBesideMainHand() || !isSlotRowShown(OFF_HAND_ROW_INDEX) || !isSlotRowShown(MAIN_HAND_ROW_INDEX)) {
            return false;
        }
        return !contentSpecForRow(player, editorPreview, OFF_HAND_ROW_INDEX).isEmpty() && !contentSpecForRow(player, editorPreview, MAIN_HAND_ROW_INDEX).isEmpty();
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
