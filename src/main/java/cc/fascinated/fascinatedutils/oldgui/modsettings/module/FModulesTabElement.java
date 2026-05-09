package cc.fascinated.fascinatedutils.oldgui.modsettings.module;

import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.oldgui.core.*;
import cc.fascinated.fascinatedutils.oldgui.modsettings.profile.FProfileCreatePopupWidget;
import cc.fascinated.fascinatedutils.oldgui.modsettings.profile.ModSettingsProfilesTabBuilder;
import cc.fascinated.fascinatedutils.oldgui.modsettings.profile.ProfilePopupController;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.oldgui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.oldgui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class FModulesTabElement extends FWidget {
    private static final float PROFILES_PANEL_WIDTH_DESIGN = 118f;
    private static final float SPLIT_DIVIDER_WIDTH_DESIGN = 1f;

    private final Runnable onProfilesChanged;
    private final Runnable onOpenHudLayoutEditor;
    private final ProfilePopupController profilePopupController;
    private final FNodeRegistry nodes = new FNodeRegistry();
    private final FNodeWidget root;

    // FState fields – null until buildRootWidget initialises them
    private FState<Float> modulesGridScrollRef;
    private FState<Float> moduleSettingsScrollRef;
    private FState<String> moduleSearchRef;
    private FState<String> moduleSettingsSearchRef;
    private FState<ModuleCategory> moduleCategoryFilterRef;
    private FState<Float> profilesScrollRef;
    private FState<String> newProfileNameRef;
    private FState<Boolean> copyDefaultProfileSettingsRef;
    private FState<Module> moduleDetailModule;
    private FState<Boolean> showCreateProfilePopup;
    private FState<FColorPickerPopupWidget> colorPickerWidget;

    // Plain mutable fields
    private Module settingsScrollAnchorModule;
    private @Nullable FOutlinedTextInputWidget moduleDetailSearchField;
    private @Nullable FOutlinedTextInputWidget moduleGridSearchInput;
    private @Nullable Module pendingModuleDetail;
    private Runnable rerenderCallback = () -> {};

    public FModulesTabElement(Runnable onProfilesChanged, Runnable onOpenHudLayoutEditor) {
        this.onProfilesChanged = onProfilesChanged;
        this.onOpenHudLayoutEditor = onOpenHudLayoutEditor;
        this.profilePopupController = new ProfilePopupController(
                () -> rerenderCallback.run(),
                this::handleProfilesChanged,
                () -> { if (profilesScrollRef != null) profilesScrollRef.set(0f); });
        this.root = new FNodeWidget(nodes.get("modules", this::buildRootWidget));
        addChild(root);
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        if (moduleDetailModule != null) {
            Module detail = moduleDetailModule.get();
            if (detail != null && !ModuleRegistry.INSTANCE.getModules().contains(detail)) {
                moduleDetailModule.set(null);
                if (moduleSettingsScrollRef != null) moduleSettingsScrollRef.setQuiet(0f);
                settingsScrollAnchorModule = null;
            }
        }
        syncScrollAnchors();
        root.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        nodes.gc();
    }

    public void reset() {
        if (modulesGridScrollRef != null) modulesGridScrollRef.set(0f);
        if (moduleSettingsScrollRef != null) moduleSettingsScrollRef.set(0f);
        if (moduleSearchRef != null) moduleSearchRef.set("");
        if (moduleSettingsSearchRef != null) moduleSettingsSearchRef.set("");
        if (moduleCategoryFilterRef != null) moduleCategoryFilterRef.set(null);
        if (profilesScrollRef != null) profilesScrollRef.set(0f);
        if (newProfileNameRef != null) newProfileNameRef.set("");
        if (copyDefaultProfileSettingsRef != null) copyDefaultProfileSettingsRef.set(false);
        if (moduleDetailModule != null) moduleDetailModule.set(null);
        if (showCreateProfilePopup != null) showCreateProfilePopup.set(false);
        if (colorPickerWidget != null) colorPickerWidget.set(null);
        settingsScrollAnchorModule = null;
        moduleDetailSearchField = null;
        moduleGridSearchInput = null;
        pendingModuleDetail = null;
        rerenderCallback = () -> {};
        profilePopupController.reset();
    }

    public void disposeDeclarativeSubtree() {
        nodes.dispose();
    }

    /**
     * Opens the modules tab detail view for the given module.
     *
     * @param module module whose settings page should be shown
     */
    public void navigateToModuleSettingsPage(Module module) {
        if (moduleDetailModule != null) {
            openModuleDetail(module);
        } else {
            pendingModuleDetail = module;
        }
    }

    private FWidget buildRootWidget(FWidgetNode.RenderContext ctx) {
        FState<Integer> rerenderToken = ctx.useState(0);
        rerenderCallback = () -> rerenderToken.update(count -> count + 1);
        Module initialDetail = pendingModuleDetail;
        pendingModuleDetail = null;
        modulesGridScrollRef = ctx.useState(0f);
        moduleSettingsScrollRef = ctx.useState(0f);
        moduleSearchRef = ctx.useState("");
        moduleSettingsSearchRef = ctx.useState("");
        moduleCategoryFilterRef = ctx.useState(null);
        profilesScrollRef = ctx.useState(0f);
        newProfileNameRef = ctx.useState("");
        copyDefaultProfileSettingsRef = ctx.useState(false);
        moduleDetailModule = ctx.useState(initialDetail);
        showCreateProfilePopup = ctx.useState(false);
        colorPickerWidget = ctx.useState(null);
        settingsScrollAnchorModule = initialDetail;
        return new FWidget() {
            private float lastWidth = Float.NaN;
            private float lastHeight = Float.NaN;

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                boolean dimChanged = Math.abs(lw - lastWidth) > 0.5f || Math.abs(lh - lastHeight) > 0.5f;
                if (dimChanged || childrenView().isEmpty()) {
                    lastWidth = lw;
                    lastHeight = lh;
                    clearChildren();
                    addChild(buildSurface(lw, lh));
                }
                for (FWidget child : childrenView()) {
                    child.layout(measure, lx, ly, lw, lh);
                }
            }
        };
    }

    private FWidget buildSurface(float width, float height) {
        Callback<Module> openModuleSettings = this::openModuleDetail;
        float splitDividerWidth = Math.max(1f, (float) Math.floor(SPLIT_DIVIDER_WIDTH_DESIGN));
        float profilesPanelWidth = Math.max(112f, Math.min(PROFILES_PANEL_WIDTH_DESIGN, width * 0.4f));
        float modulesPanelWidth = Math.max(0f, width - profilesPanelWidth - splitDividerWidth);

        FSplitRowWithDividerWidget splitLayout = new FSplitRowWithDividerWidget(profilesPanelWidth, splitDividerWidth);
        splitLayout.addChild(ModSettingsProfilesTabBuilder.buildProfilesPane(profilesPanelWidth, height, profilesScrollRef, this::openCreateProfilePopup, profilePopupController.contextMenuCallback(), this::handleProfilesChanged, onOpenHudLayoutEditor));

        FRectWidget splitDivider = new FRectWidget();
        splitDivider.setFillColorArgb(FascinatedGuiTheme.INSTANCE.borderMuted());
        splitLayout.addChild(splitDivider);

        FWidget modulesPane = ModSettingsModulesTabBuilder.buildModulesTab(modulesPanelWidth, height, List.copyOf(ModuleRegistry.INSTANCE.getModules()), modulesGridScrollRef, moduleDetailModule.get(), this::closeModuleDetail, openModuleSettings, moduleSettingsScrollRef, moduleSearchRef, moduleCategoryFilterRef, this::onModuleFiltersChanged, moduleSettingsSearchRef, this::onModuleSettingsSearchChanged, this::openColorPicker, this::lazyModuleDetailSearchField, this::lazyModuleGridSearchInput);
        splitLayout.addChild(modulesPane);

        FAbsoluteStackWidget rootStack = new FAbsoluteStackWidget();
        rootStack.addChild(splitLayout);
        if (showCreateProfilePopup.get()) {
            rootStack.addChild(new FProfileCreatePopupWidget(newProfileNameRef, copyDefaultProfileSettingsRef, this::closeCreateProfilePopup, this::submitCreateProfilePopup));
        }
        FColorPickerPopupWidget picker = colorPickerWidget.get();
        if (picker != null) {
            rootStack.addChild(picker);
        }
        profilePopupController.appendOverlaysTo(rootStack);
        return rootStack;
    }

    private void openColorPicker(ColorSetting setting) {
        colorPickerWidget.set(new FColorPickerPopupWidget(setting.getValue(), newColor -> {
            setting.setValue(newColor);
            ModConfig.profiles().saveActiveProfile();
            colorPickerWidget.set(null);
        }, () -> colorPickerWidget.set(null)));
    }

    private void openCreateProfilePopup() {
        if (showCreateProfilePopup.get()) {
            return;
        }
        showCreateProfilePopup.set(true);
        newProfileNameRef.setQuiet("");
        copyDefaultProfileSettingsRef.setQuiet(false);
    }

    private void closeCreateProfilePopup() {
        if (!showCreateProfilePopup.get()) {
            return;
        }
        showCreateProfilePopup.set(false);
    }

    private void submitCreateProfilePopup(String requestedProfileName, boolean copyDefaultProfileSettings) {
        String normalizedName = requestedProfileName == null ? "" : requestedProfileName.trim();
        if (normalizedName.isBlank()) {
            return;
        }
        if (ModConfig.profiles().profileNameExists(normalizedName)) {
            return;
        }
        ModConfig.profiles().createProfile(normalizedName, copyDefaultProfileSettings);
        showCreateProfilePopup.set(false);
        profilesScrollRef.setQuiet(0f);
        handleProfilesChanged();
    }

    private void handleProfilesChanged() {
        rerenderCallback.run();
        onProfilesChanged.run();
    }

    private void onModuleFiltersChanged() {
        modulesGridScrollRef.set(0f);
    }

    private void onModuleSettingsSearchChanged() {
        moduleSettingsScrollRef.set(0f);
    }

    private void openModuleDetail(Module module) {
        if (moduleDetailModule.get() == module) {
            return;
        }
        moduleDetailModule.set(module);
        moduleSettingsScrollRef.setQuiet(0f);
        moduleSettingsSearchRef.setQuiet("");
        settingsScrollAnchorModule = module;
    }

    private void closeModuleDetail() {
        if (moduleDetailModule.get() == null) {
            return;
        }
        moduleDetailModule.set(null);
        moduleDetailSearchField = null;
        moduleSettingsSearchRef.setQuiet("");
    }

    private FOutlinedTextInputWidget lazyModuleDetailSearchField() {
        if (moduleDetailSearchField == null) {
            moduleDetailSearchField = ModSettingsModuleDetailBuilder.createSharedModuleDetailSearchField();
        }
        return moduleDetailSearchField;
    }

    private FOutlinedTextInputWidget lazyModuleGridSearchInput() {
        if (moduleGridSearchInput == null) {
            moduleGridSearchInput = new FOutlinedTextInputWidget(180, SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN, () -> "Search modules...");
        }
        return moduleGridSearchInput;
    }

    private void syncScrollAnchors() {
        if (moduleDetailModule == null) {
            return;
        }
        Module detail = moduleDetailModule.get();
        if (detail == null) {
            return;
        }
        if (settingsScrollAnchorModule != detail) {
            if (moduleSettingsScrollRef != null) moduleSettingsScrollRef.setQuiet(0f);
            settingsScrollAnchorModule = detail;
        }
    }
}
