package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.gui.components.ScrollHost;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.settings.Settings;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * The global settings page: every registered setting outside of modules.
 */
@RequiredArgsConstructor
public class GlobalSettingsTab implements SettingsTab {

    private final SettingsTabContext context;

    @Override
    public void buildHeader(PositionedNode<?> header) {
        header.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.tab_settings")).setBold(true).alignY(0.5f));
        header.addChild(new SpacerNode());
    }

    @Override
    public void buildContents(PositionedNode<?> contents) {
        List<Setting<?>> allSettings = Settings.INSTANCE.getSettings().stream()
                .filter(setting -> setting != Settings.INSTANCE.getModuleSort())
                .toList();
        Runnable save = () -> ModConfig.config().save();
        SettingSectionsBuilder sections = new SettingSectionsBuilder(save, context.getRefresh(),
                setting -> context.getColorPicker().open(setting.getValue(), newColor -> {
                    setting.setValue(newColor);
                    save.run();
                }));
        PositionedNode<?> sectionsNode = sections.build(
                SettingCategoryGrouper.topLevelInRegistrationOrder(allSettings),
                SettingCategoryGrouper.categoriesInRegistrationOrder(allSettings));

        ScrollColumnNode list = new ScrollColumnNode().setGap(SettingsLayout.GAP).persistScroll("settings.registry");
        list.addChild(sectionsNode != null ? sectionsNode : new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.settings_empty")).setColorResolver(UiTheme::textMuted));
        contents.addChild(new ScrollHost(list, context.getLayout().getContentHeight()));
    }
}
