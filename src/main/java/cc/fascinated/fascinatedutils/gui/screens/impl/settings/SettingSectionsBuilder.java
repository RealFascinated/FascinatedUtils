package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.layout.DividerNode;
import cc.fascinated.fascinatedutils.gui.node.control.SwitchNode;
import cc.fascinated.fascinatedutils.gui.node.control.KeybindNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.control.SliderNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import net.minecraft.client.resources.language.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingSectionsBuilder.class);
    private static final Set<Class<?>> WARNED_UNSUPPORTED = new HashSet<>();

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

    /**
     * Whether the builder has a control for a setting's type. Settings of any other type are skipped, and logged once.
     *
     * @param setting the setting to check
     * @return true when a row can be built for the setting
     */
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
        List<Setting<?>> topLevel = renderableRoots(topLevelSettings);
        if (!topLevel.isEmpty()) {
            column.addChild(section(null, topLevel));
            any = true;
        }
        for (SettingCategory category : categories) {
            List<Setting<?>> renderable = renderableRoots(category.settings());
            if (renderable.isEmpty()) {
                continue;
            }
            String headerText = category.displayNameKey() == null || category.displayNameKey().isBlank() ? "" : I18n.get(category.displayNameKey());
            column.addChild(section(headerText, renderable));
            any = true;
        }
        return any ? column : null;
    }

    private static List<Setting<?>> renderableRoots(List<Setting<?>> settings) {
        List<Setting<?>> renderable = new ArrayList<>();
        for (Setting<?> setting : settings) {
            if (setting.isSubSetting()) {
                continue;
            }
            if (isRenderable(setting)) {
                renderable.add(setting);
            }
            else {
                warnUnsupported(setting);
            }
        }
        return renderable;
    }

    private static void warnUnsupported(Setting<?> setting) {
        if (WARNED_UNSUPPORTED.add(setting.getClass())) {
            LOGGER.warn("Settings screen has no control for setting type {} (first seen on '{}'); such settings are not shown",
                    setting.getClass().getSimpleName(), setting.getSettingKey());
        }
    }

    private void addRows(Setting<?> setting, int depth, List<PositionedNode<?>> rows) {
        rows.add(buildRow(setting, depth));
        boolean childrenShown = !(setting instanceof BooleanSetting booleanSetting) || booleanSetting.isEnabled();
        if (!setting.hasSubSettings() || !childrenShown) {
            return;
        }
        for (Setting<?> subSetting : setting.getSubSettings()) {
            if (isRenderable(subSetting)) {
                addRows(subSetting, depth + 1, rows);
            }
            else {
                warnUnsupported(subSetting);
            }
        }
    }

    private PositionedNode<?> section(String heading, List<Setting<?>> settings) {
        List<PositionedNode<?>> rows = new ArrayList<>();
        for (Setting<?> setting : settings) {
            addRows(setting, 0, rows);
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
            String upperHeading = heading.toUpperCase(Locale.ROOT);
            group.addChild(new TextNode(() -> upperHeading)
                    .setBold(true)
                    .setColorResolver(UiTheme::textSubtle)
                    .setTextAlign(0f, 0.5f)
                    .fullWidth()
                    .height(HEADING_HEIGHT));
        }
        group.addChild(card);
        return group;
    }

    private PositionedNode<?> buildRow(Setting<?> setting, int depth) {
        PositionedNode<?> row = new PositionedNode<>().fullWidth().height(ROW_HEIGHT).rowGap(ROW_GAP);
        row.left(ROW_PADDING + depth * SUB_SETTING_INDENT).right(ROW_PADDING);
        row.addChild(new TextNode(setting::getName)
                .setColorResolver(theme -> setting.isLocked() ? theme.textSubtle() : depth > 0 ? theme.textMuted() : theme.textPrimary())
                .alignY(0.5f));
        row.addChild(new SpacerNode());

        switch (setting) {
            case BooleanSetting booleanSetting -> row.addChild(booleanControl(booleanSetting));
            case SliderSetting sliderSetting -> {
                row.addChild(new TextNode(sliderSetting::formatValueForDisplay).setColorResolver(UiTheme::textMuted).width(VALUE_WIDTH).alignY(0.5f));
                row.addChild(sliderControl(sliderSetting));
            }
            case EnumSetting<?> enumSetting -> row.addChild(enumControl(enumSetting));
            case KeybindSetting keybindSetting ->
                    row.addChild(new KeybindNode(keybindSetting).size(BUTTON_CONTROL_WIDTH, CONTROL_HEIGHT).alignY(0.5f));
            case ColorSetting colorSetting -> {
                row.addChild(new RectNode().size(SWATCH_SIZE, SWATCH_SIZE).setCornerRadius(3).setFillSupplier(colorSetting::getResolvedArgb).setBorderResolver(UiTheme::inputBorder).alignY(0.5f));
                row.addChild(colorControl(colorSetting));
            }
            default -> {
            }
        }
        row.setTooltip(() -> rowTooltip(setting));
        return row;
    }

    private String rowTooltip(Setting<?> setting) {
        if (setting.isLocked()) {
            Supplier<String> lockedReason = setting.getLockedReason();
            String reason = lockedReason != null ? lockedReason.get() : null;
            return reason != null && !reason.isBlank() ? reason : UiTranslations.INSTANCE.get("alumite.setting.ui.lock_reason.default");
        }
        return setting.getTooltipProvider().get();
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
        slider.setStep(setting.getStep());
        slider.setDisabled(setting::isLocked).setOnCommit(save);
        slider.width(SLIDER_WIDTH).alignY(0.5f);
        return slider;
    }

    private ButtonNode enumControl(EnumSetting<?> setting) {
        ButtonNode button = new ButtonNode(setting::formatValueForDisplay)
                .setDisabled(setting::isLocked)
                .setOnPress(() -> {
                    setting.cycleNextConstant();
                    save.run();
                });
        button.size(BUTTON_CONTROL_WIDTH, CONTROL_HEIGHT).alignY(0.5f);
        return button;
    }

    private ButtonNode colorControl(ColorSetting setting) {
        ButtonNode button = new ButtonNode(() -> Colors.rgbHex(setting.getValue().getPackedArgb()))
                .setDisabled(setting::isLocked)
                .setOnPress(() -> openColorPicker.accept(setting));
        button.size(HEX_BUTTON_WIDTH, CONTROL_HEIGHT).alignY(0.5f);
        return button;
    }
}
