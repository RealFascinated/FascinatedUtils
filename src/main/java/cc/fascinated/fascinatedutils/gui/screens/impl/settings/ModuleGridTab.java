package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.gui.components.InputHost;
import cc.fascinated.fascinatedutils.gui.components.ScrollHost;
import cc.fascinated.fascinatedutils.gui.components.SurfaceCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextureNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.settings.Settings;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleIcons;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The module grid page: searchable, filterable and sortable tiles that switch modules on and off or open their detail
 * page.
 */
@RequiredArgsConstructor
public class ModuleGridTab implements SettingsTab {

    public static final String SEARCH_NODE_ID = "modules.search";

    private static final int SEARCH_WIDTH = 190;
    private static final int ENABLED_COUNT_MIN_MAIN_WIDTH = 560;
    private static final int TILE_HEIGHT = 132;
    private static final int TILE_ICON_SIZE = 40;
    private static final int TILE_ICON_TOP = 10;
    private static final int TILE_NAME_HEIGHT = 12;
    private static final int TILE_NAME_GAP = 8;
    private static final int EMPTY_STATE_TOP = 64;

    private final SettingsTabContext context;

    @Override
    public void buildHeader(PositionedNode<?> header) {
        SettingsState state = context.getState();
        UiState<String> search = state.moduleSearch();
        UiState<ModuleCategory> filter = state.moduleFilter();
        TextInputNode<String> searchInput = new TextInputNode<>(TextParser.STRING)
                .setPlaceholder(UiTranslations.INSTANCE.get("alumite.setting.shell.search_modules"))
                .setOnChange(search::set)
                .setValue(search.get());
        searchInput.setNodeId(SEARCH_NODE_ID);
        header.addChild(new InputHost(searchInput, SEARCH_WIDTH));
        PositionedNode<?> chips = new PositionedNode<>().height(SettingsLayout.CONTROL_HEIGHT).rowGap(UiMetrics.SPACE_XS);
        chips.addChild(filterButton(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.filter_all"), null, filter));
        for (ModuleCategory category : ModuleCategory.values()) {
            chips.addChild(filterButton(category::getDisplayName, category, filter));
        }
        header.addChild(chips);
        header.addChild(new SpacerNode());
        EnumSetting<ModuleSort> sort = Settings.INSTANCE.getModuleSort();
        header.addChild(new ButtonNode(() -> Component.translatable("alumite.setting.shell.sort", sort.getValue().displayName()).getString())
                .setVariant(ButtonNode.ButtonVariant.DEFAULT)
                .setOnPress(() -> {
                    sort.cycleNextConstant();
                    ModConfig.config().save();
                }));
        if (context.getLayout().getMainWidth() < ENABLED_COUNT_MIN_MAIN_WIDTH) {
            return;
        }
        List<Module> allModules = ModuleRegistry.INSTANCE.getModules().stream().filter(module -> !module.isHidden()).toList();
        header.addChild(new TextNode(() -> Component.translatable("alumite.setting.shell.enabled_count",
                allModules.stream().filter(Module::isEnabled).count(), allModules.size()).getString())
                .setColorResolver(UiTheme::textSubtle)
                .alignY(0.5f));
    }

    @Override
    public void buildContents(PositionedNode<?> contents) {
        SettingsState state = context.getState();
        UiState<String> search = state.moduleSearch();
        UiState<ModuleCategory> filter = state.moduleFilter();

        String searchLower = search.get().toLowerCase(Locale.ROOT);
        List<Module> modules = new ArrayList<>(ModuleRegistry.INSTANCE.getModules().stream()
                .filter(module -> !module.isHidden())
                .filter(module -> filter.get() == null || module.getCategory() == filter.get())
                .filter(module -> searchLower.isEmpty() || module.matchesSearch(searchLower))
                .toList());
        Comparator<Module> byName = (left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getDisplayName(), right.getDisplayName());
        switch (Settings.INSTANCE.getModuleSort().getValue()) {
            case LAST_USED -> modules.sort(Comparator.comparingLong(Module::getLastUsedMillis).reversed().thenComparing(byName));
            case ENABLED_FIRST -> modules.sort(Comparator.comparing((Module module) -> !module.isEnabled()).thenComparing(byName));
            default -> modules.sort(byName);
        }

        SettingsLayout layout = context.getLayout();
        ScrollColumnNode list = new ScrollColumnNode().setGap(SettingsLayout.GAP).persistScroll("modules.grid");
        if (modules.isEmpty()) {
            list.addChild(noResultsState(search, filter));
        }
        int columns = layout.getGridColumns();
        for (int index = 0; index < modules.size(); index += columns) {
            PositionedNode<?> row = new PositionedNode<>().fullWidth().height(TILE_HEIGHT).rowGap(SettingsLayout.GAP);
            for (int column = index; column < Math.min(index + columns, modules.size()); column++) {
                row.addChild(moduleTile(modules.get(column)));
            }
            list.addChild(row);
        }
        contents.addChild(new ScrollHost(list, layout.getContentHeight()));
    }

    private PositionedNode<?> noResultsState(UiState<String> search, UiState<ModuleCategory> filter) {
        PositionedNode<?> state = new PositionedNode<>().fullWidth().columnGap(UiMetrics.SPACE_SM);
        state.addChild(new PositionedNode<>().fullWidth().height(EMPTY_STATE_TOP));
        state.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.no_results_title"))
                .setBold(true)
                .alignX(0.5f));
        state.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.no_results_hint"))
                .setColorResolver(UiTheme::textMuted)
                .alignX(0.5f));
        state.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.clear_filters"))
                .alignX(0.5f)
                .setOnPress(() -> {
                    search.set("");
                    filter.set(null);
                }));
        return state;
    }

    private ButtonNode filterButton(Supplier<String> label, @Nullable ModuleCategory category, UiState<ModuleCategory> filter) {
        return new ButtonNode(label)
                .setVariant(() -> filter.get() == category ? ButtonNode.ButtonVariant.DEFAULT : ButtonNode.ButtonVariant.GHOST)
                .setLabelColorResolver(theme -> filter.get() == category ? theme.textPrimary() : theme.textMuted())
                .setOnPress(() -> filter.set(category));
    }

    private CardNode moduleTile(Module module) {
        int tileWidth = context.getLayout().getTileWidth();
        CardNode tile = new SurfaceCard().width(tileWidth).height(TILE_HEIGHT);
        tile.setContents(content -> {
            content.addChild(new TextureNode()
                    .setTexture(ModuleIcons.INSTANCE.iconFor(module))
                    .setTintResolver(theme -> module.isEnabled() ? theme.textPrimary() : theme.textSubtle())
                    .size(TILE_ICON_SIZE).alignX(0.5f).top(TILE_ICON_TOP));
            content.addChild(new TextNode(module::getDisplayName)
                    .setBold(true)
                    .setColorResolver(theme -> module.isEnabled() ? theme.textPrimary() : theme.textMuted())
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth().height(TILE_NAME_HEIGHT)
                    .top(TILE_ICON_TOP + TILE_ICON_SIZE + TILE_NAME_GAP));

            ButtonNode options = new ButtonNode(() -> UiTranslations.INSTANCE.getUpper("alumite.setting.shell.module_options"))
                    .setCornerMask(RectCornerRoundMask.TOP)
                    .setOnPress(() -> openModuleDetail(module));
            options.size(tileWidth, SettingsLayout.CONTROL_HEIGHT).left(0).bottom(SettingsLayout.CONTROL_HEIGHT);
            content.addChild(options);

            // Enabled is a solid green bar, disabled a solid red one.
            ButtonNode status = new ButtonNode(() -> UiTranslations.INSTANCE.getUpper(module.isEnabled()
                    ? "alumite.setting.shell.widget_state.enabled" : "alumite.setting.shell.widget_state.disabled"))
                    .setCornerMask(RectCornerRoundMask.BOTTOM)
                    .setVariant(() -> module.isEnabled() ? ButtonNode.ButtonVariant.SUCCESS : ButtonNode.ButtonVariant.DANGER)
                    .setOnPress(() -> {
                        ModuleRegistry.INSTANCE.setModuleEnabled(module, !module.isEnabled());
                        module.markUsed();
                        ModConfig.profiles().saveModule(module);
                    });
            status.setDisabled(() -> module.getActiveRestriction().isPresent());
            status.setTooltip(() -> context.restrictionReason(module));
            status.size(tileWidth, SettingsLayout.CONTROL_HEIGHT).left(0).bottom(0);
            content.addChild(status);
        });
        return tile;
    }

    private void openModuleDetail(Module module) {
        SettingsState state = context.getState();
        state.detailSearch().set("");
        module.markUsed();
        ModConfig.profiles().saveModule(module);
        state.openModule().set(module);
    }
}
