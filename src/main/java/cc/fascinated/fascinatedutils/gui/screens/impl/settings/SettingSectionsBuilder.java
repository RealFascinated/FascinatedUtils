package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.CardNode;
import cc.fascinated.fascinatedutils.gui.node.DividerNode;
import cc.fascinated.fascinatedutils.gui.node.SwitchNode;
import cc.fascinated.fascinatedutils.gui.node.KeybindNode;
import cc.fascinated.fascinatedutils.gui.node.RectNode;
import cc.fascinated.fascinatedutils.gui.node.SliderNode;
import cc.fascinated.fascinatedutils.gui.node.TextNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Builds the scrollable list of setting rows (top-level settings followed by category sections) used by the
 * module detail page and the global settings tab.
 */
public class SettingSectionsBuilder {

    private static final int ROW_HEIGHT = 32;
    private static final int ROW_GAP = UiMetrics.SPACE_SM;
    private static final int ROW_PADDING = UiMetrics.SPACE_MD;
    private static final int CONTROL_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int SECTION_GAP = UiMetrics.SPACE_XS;
    private static final int GROUP_GAP = UiMetrics.SPACE_MD;
    private static final int SUB_SETTING_INDENT = UiMetrics.SPACE_MD;
    private static final int SLIDER_WIDTH = 120;
    private static final int VALUE_WIDTH = 44;
    private static final int BUTTON_CONTROL_WIDTH = 110;
    private static final int CHECKBOX_WIDTH = SwitchNode.TRACK_WIDTH;
    private static final int HEADING_HEIGHT = 12;
    private static final int SWATCH_SIZE = 12;
    private static final int HEX_BUTTON_WIDTH = 70;

    private final Runnable save;
    private final Runnable refresh;
    private final Consumer<ColorSetting> openColorPicker;

    /**
     * Creates a builder bound to one persistence target.
     *
     * @param save            persists the owning module or registry after a setting changed
     * @param refresh         rebuilds the screen when a change alters which rows are visible
     * @param openColorPicker opens the shared color picker for a color setting
     */
    public SettingSectionsBuilder(Runnable save, Runnable refresh, Consumer<ColorSetting> openColorPicker) {
        this.save = save;
        this.refresh = refresh;
        this.openColorPicker = openColorPicker;
    }

    public static boolean isRenderable(Setting<?> setting) {
        return setting instanceof BooleanSetting || setting instanceof SliderSetting || setting instanceof KeybindSetting
                || setting instanceof EnumSetting<?> || setting instanceof ColorSetting;
    }

    /**
     * Builds the sections column.
     *
     * @param topLevelSettings settings registered outside any category
     * @param categories       category blocks shown after the top-level settings
     * @return a full-width column node, or {@code null} when nothing is renderable
     */
    public PositionedNode<?> build(List<Setting<?>> topLevelSettings, List<SettingCategory> categories) {
        PositionedNode<?> column = new PositionedNode<>().fullWidth().columnGap(GROUP_GAP);
        boolean any = false;
        List<Setting<?>> topLevel = topLevelSettings.stream().filter(setting -> !setting.isSubSetting() && isRenderable(setting)).toList();
        if (!topLevel.isEmpty()) {
            column.addChild(section(null, topLevel));
            any = true;
        }
        for (SettingCategory category : categories) {
            List<Setting<?>> renderable = category.settings().stream().filter(setting -> !setting.isSubSetting() && isRenderable(setting)).toList();
            if (renderable.isEmpty()) {
                continue;
            }
            String headerText = category.displayNameKey() == null || category.displayNameKey().isBlank() ? "" : I18n.get(category.displayNameKey());
            column.addChild(section(headerText, renderable));
            any = true;
        }
        return any ? column : null;
    }

    private PositionedNode<?> section(String heading, List<Setting<?>> settings) {
        List<PositionedNode<?>> rows = new ArrayList<>();
        for (Setting<?> setting : settings) {
            rows.add(buildRow(setting, false));
            if (setting instanceof BooleanSetting booleanSetting && booleanSetting.hasSubSettings() && booleanSetting.isEnabled()) {
                for (Setting<?> subSetting : booleanSetting.getSubSettings()) {
                    if (isRenderable(subSetting)) {
                        rows.add(buildRow(subSetting, true));
                    }
                }
            }
        }
        int cardHeight = rows.size() * ROW_HEIGHT + (rows.size() - 1);
        CardNode card = new CardNode().fullWidth().height(cardHeight);
        card.setFillResolver(UiTheme::surfaceCard);
        card.setBorderResolver(UiTheme::borderSubtle);
        card.setContents(contents -> {
            contents.columnGap(0);
            for (int index = 0; index < rows.size(); index++) {
                if (index > 0) {
                    contents.addChild(new DividerNode().fullWidth().height(1));
                }
                contents.addChild(rows.get(index));
            }
        });
        PositionedNode<?> group = new PositionedNode<>().fullWidth().height(cardHeight + (heading != null && !heading.isBlank() ? HEADING_HEIGHT + SECTION_GAP : 0)).columnGap(SECTION_GAP);
        if (heading != null && !heading.isBlank()) {
            group.addChild(new TextNode(() -> heading.toUpperCase(Locale.ROOT))
                    .setBold(true)
                    .setColorResolver(UiTheme::textSubtle)
                    .setTextAlign(0f, 0.5f)
                    .fullWidth()
                    .height(HEADING_HEIGHT));
        }
        group.addChild(card);
        return group;
    }

    private PositionedNode<?> buildRow(Setting<?> setting, boolean indented) {
        PositionedNode<?> row = new PositionedNode<>().fullWidth().height(ROW_HEIGHT).rowGap(ROW_GAP);
        row.left(ROW_PADDING + (indented ? SUB_SETTING_INDENT : 0)).right(ROW_PADDING);
        row.addChild(new TextNode(setting::getName)
                .setColorResolver(theme -> setting.isLocked() ? theme.textSubtle() : indented ? theme.textMuted() : theme.textPrimary())
                .alignY(0.5f));
        row.addChild(new SpacerNode());

        if (setting instanceof BooleanSetting booleanSetting) {
            row.addChild(booleanControl(booleanSetting));
        } else if (setting instanceof SliderSetting sliderSetting) {
            row.addChild(new TextNode(sliderSetting::formatValueForDisplay).setColorResolver(UiTheme::textMuted).width(VALUE_WIDTH).alignY(0.5f));
            row.addChild(sliderControl(sliderSetting));
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            row.addChild(enumControl(enumSetting));
        } else if (setting instanceof KeybindSetting keybindSetting) {
            row.addChild(new KeybindNode(keybindSetting).size(BUTTON_CONTROL_WIDTH, CONTROL_HEIGHT).alignY(0.5f));
        } else if (setting instanceof ColorSetting colorSetting) {
            row.addChild(new RectNode()
                    .size(SWATCH_SIZE, SWATCH_SIZE)
                    .setCornerRadius(3)
                    .setFillSupplier(colorSetting::getResolvedArgb)
                    .setBorderResolver(UiTheme::inputBorder)
                    .alignY(0.5f));
            row.addChild(colorControl(colorSetting));
        }
        return row;
    }

    private SwitchNode booleanControl(BooleanSetting setting) {
        SwitchNode checkbox = new SwitchNode().setChecked(setting.isEnabled());
        checkbox.width(CHECKBOX_WIDTH).alignY(0.5f);
        checkbox.setOnChange(checked -> {
            if (setting.isLocked()) {
                checkbox.setChecked(setting.isEnabled());
                return;
            }
            setting.setValue(checked);
            save.run();
            if (setting.hasSubSettings()) {
                refresh.run();
            }
        });
        return checkbox;
    }

    private SliderNode sliderControl(SliderSetting setting) {
        SliderNode slider = new SliderNode(setting.getMin(), setting.getMax(), () -> setting.getValue().floatValue(), rawValue -> {
            if (!setting.isLocked()) {
                setting.setValue(rawValue);
            }
        });
        slider.setDisabled(setting::isLocked).setOnCommit(save);
        slider.width(SLIDER_WIDTH).alignY(0.5f);
        return slider;
    }

    private ButtonNode enumControl(EnumSetting<?> setting) {
        ButtonNode button = new ButtonNode(setting::formatValueForDisplay)
                .setRounded(true)
                .setDisabled(setting::isLocked)
                .setOnPress(() -> {
                    setting.cycleNextConstant();
                    save.run();
                });
        button.size(BUTTON_CONTROL_WIDTH, CONTROL_HEIGHT).alignY(0.5f);
        return button;
    }

    private ButtonNode colorControl(ColorSetting setting) {
        ButtonNode button = new ButtonNode(() -> "#" + String.format("%06X", setting.getValue().getPackedArgb() & 0xFFFFFF))
                .setRounded(true)
                .setDisabled(setting::isLocked)
                .setOnPress(() -> openColorPicker.accept(setting));
        button.size(HEX_BUTTON_WIDTH, CONTROL_HEIGHT).alignY(0.5f);
        return button;
    }
}
