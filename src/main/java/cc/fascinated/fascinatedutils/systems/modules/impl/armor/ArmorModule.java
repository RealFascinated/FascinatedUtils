package cc.fascinated.fascinatedutils.systems.modules.impl.armor;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.config.ConfigVersion;
import cc.fascinated.fascinatedutils.systems.modules.impl.armor.hud.ArmorHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.Module;

@ConfigVersion(2)
public class ArmorModule extends HudHostModule {

    public static final int ARMOR_HUD_ROW_COUNT = 6;
    private static final String SLOTS_CATEGORY_DISPLAY_KEY = "Slots";

    private final BooleanSetting[] slotRowVisibility = {
            BooleanSetting.builder().id("show_head").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_chest").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_legs").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_feet").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_off_hand").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build(),
            BooleanSetting.builder().id("show_main_hand").defaultValue(true).categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY).build()
    };

    private final BooleanSetting showItemStackSizeBytes = BooleanSetting.builder()
            .id("show_item_stack_size_bytes")
            .defaultValue(false)
            .categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showOffHandNextToMainHand = BooleanSetting.builder()
            .id("show_off_hand_next_to_main_hand")
            .defaultValue(false)
            .categoryDisplayKey(SLOTS_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting hideUnbreakableDurability = BooleanSetting.builder()
            .id("hide_unbreakable_durability")
            .defaultValue(false)
            .categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showTotalInventoryCount = BooleanSetting.builder()
            .id("show_total_item_count")
            .defaultValue(true)
            .categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    private final BooleanSetting colorArmorDurability = BooleanSetting.builder()
            .id("color_armor_durability")
            .defaultValue(true)
            .categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting colorMainHandDurability = BooleanSetting.builder()
            .id("color_main_hand_durability")
            .defaultValue(true)
            .categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting colorOffHandDurability = BooleanSetting.builder()
            .id("color_off_hand_durability")
            .defaultValue(true)
            .categoryDisplayKey(Module.APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public ArmorModule() {
        super("armor_hud", "Armor HUD", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.BOTTOM_RIGHT).defaultXOffset(5).defaultYOffset(5).build());
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
        int offHandSlotIndex = 4;
        for (int slotIndex = 0; slotIndex < ARMOR_HUD_ROW_COUNT; slotIndex++) {
            addSetting(slotRowVisibility[slotIndex]);
        }
        addSetting(showOffHandNextToMainHand);
        addSetting(hideUnbreakableDurability);
        addSetting(showTotalInventoryCount);
        addSetting(colorArmorDurability);
        addSetting(colorMainHandDurability);
        addSetting(colorOffHandDurability);
        addSetting(showItemStackSizeBytes);
        slotRowVisibility[offHandSlotIndex].addSubSetting(showOffHandNextToMainHand);
        registerHudPanel(new ArmorHudPanel(this));
    }

    public boolean armorHudSlotRowEnabled(int rowIndex) {
        return rowIndex >= 0 && rowIndex < ARMOR_HUD_ROW_COUNT && slotRowVisibility[rowIndex].isEnabled();
    }

    public boolean armorHudShowOffHandBesideMainHand() {
        return showOffHandNextToMainHand.isEnabled();
    }

    public boolean armorHudHideUnbreakableDurability() {
        return hideUnbreakableDurability.isEnabled();
    }

    public boolean armorHudShowTotalInventoryCount() {
        return showTotalInventoryCount.isEnabled();
    }

    public boolean armorHudColorArmorDurability() {
        return colorArmorDurability.isEnabled();
    }

    public boolean armorHudColorMainHandDurability() {
        return colorMainHandDurability.isEnabled();
    }

    public boolean armorHudColorOffHandDurability() {
        return colorOffHandDurability.isEnabled();
    }

    public boolean armorHudShowItemStackSizeBytes() {
        return showItemStackSizeBytes.isEnabled();
    }
}
