package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class FProfilesTabElement extends FWidget {
    private final Ref<Float> profilesScrollRef = Ref.of(0f);
    private final Ref<String> profileNameInputRef = Ref.of("");
    private final Runnable onProfilesChanged;
    private final ProfilePopupController profilePopupController;
    private FWidget inner;
    private boolean dirty = true;
    private float cachedWidth = -1f;
    private float cachedHeight = -1f;

    public FProfilesTabElement(Runnable onProfilesChanged) {
        this.onProfilesChanged = onProfilesChanged;
        this.profilePopupController = new ProfilePopupController(
                () -> dirty = true,
                this::handleProfilesChanged,
                () -> profilesScrollRef.setValue(0f)
        );
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
        if (dirty || cachedWidth != layoutWidth || cachedHeight != layoutHeight || inner == null) {
            FWidget profilesPane = ModSettingsProfilesTabBuilder.buildProfilesTab(layoutWidth, layoutHeight, profilesScrollRef, profileNameInputRef, profilePopupController.contextMenuCallback(), this::handleProfilesChanged);
            FAbsoluteStackWidget stack = new FAbsoluteStackWidget();
            stack.addChild(profilesPane);
            profilePopupController.appendOverlaysTo(stack);
            inner = stack;
            clearChildren();
            addChild(inner);
            cachedWidth = layoutWidth;
            cachedHeight = layoutHeight;
            dirty = false;
        }
        if (inner != null) {
            inner.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }

    public void reset() {
        profilesScrollRef.setValue(0f);
        profileNameInputRef.setValue("");
        inner = null;
        dirty = true;
        cachedWidth = -1f;
        cachedHeight = -1f;
        profilePopupController.reset();
        clearChildren();
    }

    private void handleProfilesChanged() {
        dirty = true;
        onProfilesChanged.run();
    }
}

