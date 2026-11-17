package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModSettingsScreen;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.node.HudPanelNode;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * Editor node for one HUD panel: wraps the same {@link HudPanelNode} the game draws, lets the pointer drag it, and owns
 * the action buttons and scale handle as child nodes. The panel's own node is laid out first, so the wrapper, buttons
 * and handle follow whatever position and size the panel ended up with.
 */
public class HudEditorPanelNode extends UiNode {

    private static final int ACTION_ICON_SIZE = 10;

    /**
     * Panel edited by this node.
     */
    @Getter
    private final HudPanel panel;
    private final HudEditorPointerSession session;
    private final Supplier<HudPanel> actionOwner;
    private final HudPanelNode panelNode;
    private final ButtonNode settingsButton;
    private final ButtonNode closeButton;
    private final HudScaleHandleNode scaleHandle;

    /**
     * Creates the node for one panel.
     *
     * @param panel           panel being edited
     * @param panelNode       the panel's node, composed for this frame exactly as the game composes it
     * @param session         shared editor state
     * @param returnScreen    screen the settings shell returns to
     * @param actionOwner     supplies the panel that currently shows the settings and close buttons
     * @param showScaleHandle whether the scale handle is shown
     */
    public HudEditorPanelNode(HudPanel panel, HudPanelNode panelNode, HudEditorPointerSession session, Screen returnScreen, Supplier<HudPanel> actionOwner, boolean showScaleHandle) {
        this.panel = panel;
        this.panelNode = panelNode;
        this.session = session;
        this.actionOwner = actionOwner;
        settingsButton = new ButtonNode()
                .setIconCenter(ModUiTextures.SETTINGS.getId())
                .setIconSize(ACTION_ICON_SIZE)
                .setOnPress(() -> Minecraft.getInstance().setScreen(new ModSettingsScreen(panel.hudSettingsNavigationTarget(), returnScreen)))
                .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.hud_editor.panel_settings"));
        settingsButton.size(HudEditorChrome.ACTION_BUTTON_SIZE);
        closeButton = new ButtonNode()
                .setIconCenter(ModUiTextures.CLOSE.getId())
                .setIconSize(ACTION_ICON_SIZE)
                .setVariant(ButtonNode.ButtonVariant.DANGER)
                .setOnPress(() -> session.disable(panel))
                .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.hud_editor.panel_disable"));
        closeButton.size(HudEditorChrome.ACTION_BUTTON_SIZE);
        addChild(panelNode);
        addChild(settingsButton);
        addChild(closeButton);
        if (showScaleHandle) {
            scaleHandle = new HudScaleHandleNode(panel, session);
            addChild(scaleHandle);
        }
        else {
            scaleHandle = null;
        }
    }

    @Override
    public void layout(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        int parentWidth = Math.round(HudLayoutCanvas.width());
        int parentHeight = Math.round(HudLayoutCanvas.height());
        panelNode.layout(renderFrame, 0, 0, parentWidth, parentHeight);
        bounds().set(panelNode.bounds().positionX(), panelNode.bounds().positionY(), panelNode.bounds().width(), panelNode.bounds().height());

        float panelX = panel.getHudState().getPositionX();
        float panelY = panel.getHudState().getPositionY();
        float scaledWidth = panel.getScaledWidth();
        boolean showActions = actionOwner.get() == panel;
        settingsButton.setVisible(showActions);
        closeButton.setVisible(showActions);
        if (showActions) {
            int barY = Math.round(HudEditorChrome.buttonBarY(panelY, panel.getScaledHeight())) + HudEditorChrome.ACTION_BUTTON_PAD;
            settingsButton.pos(Math.round(panelX) + HudEditorChrome.ACTION_BUTTON_PAD, barY);
            closeButton.pos(Math.round(panelX + scaledWidth) - HudEditorChrome.ACTION_BUTTON_SIZE - HudEditorChrome.ACTION_BUTTON_PAD, barY);
            settingsButton.layout(renderFrame, 0, 0, parentWidth, parentHeight);
            closeButton.layout(renderFrame, 0, 0, parentWidth, parentHeight);
        }
        if (scaleHandle != null) {
            scaleHandle.layout(renderFrame, 0, 0, parentWidth, parentHeight);
        }
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        session.beginDrag(panel, pointerX, pointerY);
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (session.dragging() != panel) {
            return false;
        }
        session.dragTo(pointerX, pointerY);
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button != 0 || session.dragging() != panel) {
            return false;
        }
        session.endInteraction();
        return true;
    }
}
