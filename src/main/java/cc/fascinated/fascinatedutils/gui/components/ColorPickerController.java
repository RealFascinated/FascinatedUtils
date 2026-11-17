package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.gui.node.popup.ColorPickerPopupNode;

import java.util.function.Consumer;

/**
 * Opens and closes the shared color picker popup for a screen. The popup is composed fresh on every compose and keeps
 * its own editing state across recomposition; this controller only remembers what to edit and what to do with the
 * result. The open flag itself is ordinary screen state.
 */
public class ColorPickerController {

    private static final String POPUP_NODE_ID = "color-picker-popup";

    private final UiStateStore stateStore;
    private final String stateKey;
    private SettingColor startColor;
    private Consumer<SettingColor> onApply;

    /**
     * Creates a controller whose open flag lives in a screen's state store.
     *
     * @param stateStore the screen's state store
     * @param stateKey   key of the open flag, unique within the store
     */
    public ColorPickerController(UiStateStore stateStore, String stateKey) {
        this.stateStore = stateStore;
        this.stateKey = stateKey;
    }

    private UiState<Boolean> openState() {
        return stateStore.state(stateKey, false);
    }

    /**
     * Opens the picker on a color.
     *
     * @param color   the color to start editing, left untouched
     * @param onApply receives a copy of the chosen color when the user applies it
     */
    public void open(SettingColor color, Consumer<SettingColor> onApply) {
        this.startColor = color.copy();
        this.onApply = onApply;
        openState().set(true);
    }

    /**
     * Closes the picker without applying.
     */
    public void close() {
        openState().set(false);
        startColor = null;
        onApply = null;
    }

    /**
     * Whether the picker is open.
     *
     * @return true while the popup should be part of the composed tree
     */
    public boolean isOpen() {
        return openState().get() && startColor != null;
    }

    /**
     * Composes the popup to add to the tree.
     *
     * @return a new popup, or null when the picker is closed
     */
    public UiNode popupOrNull() {
        if (!isOpen()) {
            return null;
        }
        Consumer<SettingColor> applyAction = onApply;
        return new ColorPickerPopupNode(startColor, chosen -> {
            applyAction.accept(chosen);
            close();
        }, this::close).setNodeId(POPUP_NODE_ID);
    }
}
