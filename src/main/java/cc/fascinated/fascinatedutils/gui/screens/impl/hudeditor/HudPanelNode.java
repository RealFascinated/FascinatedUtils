package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModSettingsScreen;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Editor node for one HUD panel: draws the live preview, lets the pointer drag it, and owns the action buttons and
 * scale handle as child nodes.
 */
public class HudPanelNode extends UiNode {

    private static final int ACTION_ICON_SIZE = 10;

    private final HudPanel panel;
    private final HudEditorMergeGroup mergeGroup;
    private final HudEditorPointerSession session;
    private final float deltaSeconds;
    private final boolean repositionFromAnchor;
    private final ButtonNode settingsButton;
    private final ButtonNode closeButton;
    private final HudScaleHandleNode scaleHandle;
    @Nullable
    private Runnable drawPreview;

    /**
     * Creates the node for one panel.
     *
     * @param panel                panel being edited
     * @param session              shared editor state
     * @param returnScreen         screen the settings shell returns to
     * @param deltaSeconds         frame delta used to prepare the preview
     * @param repositionFromAnchor whether the panel should follow its anchor before drawing
     * @param showActions          whether the settings and close buttons are shown
     * @param showScaleHandle      whether the scale handle is shown
     * @param mergeGroup           group that merges the corners of touching panels
     */
    public HudPanelNode(HudPanel panel, HudEditorPointerSession session, Screen returnScreen, float deltaSeconds, boolean repositionFromAnchor, boolean showActions, boolean showScaleHandle, HudEditorMergeGroup mergeGroup) {
        this.panel = panel;
        this.mergeGroup = mergeGroup;
        mergeGroup.register(this);
        this.session = session;
        this.deltaSeconds = deltaSeconds;
        this.repositionFromAnchor = repositionFromAnchor;
        if (showActions) {
            settingsButton = new ButtonNode()
                    .setIconCenter(ModUiTextures.SETTINGS.getId())
                    .setIconSize(ACTION_ICON_SIZE)
                    .setOnPress(() -> Minecraft.getInstance().setScreen(new ModSettingsScreen(panel.hudSettingsNavigationTarget(), returnScreen)))
                    .setTooltip(() -> Component.translatable("alumite.hud_editor.panel_settings").getString());
            settingsButton.size(HudEditorChrome.ACTION_BUTTON_SIZE);
            closeButton = new ButtonNode()
                    .setIconCenter(ModUiTextures.CLOSE.getId())
                    .setIconSize(ACTION_ICON_SIZE)
                    .setVariant(ButtonNode.ButtonVariant.DANGER)
                    .setOnPress(() -> session.disable(panel))
                    .setTooltip(() -> Component.translatable("alumite.hud_editor.panel_disable").getString());
            closeButton.size(HudEditorChrome.ACTION_BUTTON_SIZE);
            addChild(settingsButton);
            addChild(closeButton);
        }
        else {
            settingsButton = null;
            closeButton = null;
        }
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
        drawPreview = panel.prepareAndDraw(renderFrame, deltaSeconds, true);
        if (drawPreview == null) {
            panel.recordHudContentSkipped();
        }
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        if (repositionFromAnchor) {
            panel.applyHudAnchorToPosition(canvasWidth, canvasHeight);
        }
        float panelX = panel.getHudState().getPositionX();
        float panelY = panel.getHudState().getPositionY();
        float scaledWidth = panel.getScaledWidth();
        float scaledHeight = panel.getScaledHeight();
        bounds().set(Math.round(panelX), Math.round(panelY), Math.round(scaledWidth), Math.round(scaledHeight));

        int parentWidth = Math.round(canvasWidth);
        int parentHeight = Math.round(canvasHeight);
        if (settingsButton != null) {
            int barY = Math.round(HudEditorChrome.buttonBarY(panelY, scaledHeight)) + HudEditorChrome.ACTION_BUTTON_PAD;
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
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        if (drawPreview == null) {
            return;
        }
        mergeGroup.updateIfLeader(this);
        panel.hudHostModule().setSquaredCornerMask(panel.getHudState().getSquaredCornerMask());
        renderFrame.pushTranslate(Math.round(panel.getHudState().getPositionX()), Math.round(panel.getHudState().getPositionY()));
        renderFrame.pushScale(panel.getHudState().getScale());
        drawPreview.run();
        renderFrame.endRenderSegment();
        renderFrame.popScale();
        renderFrame.popTranslate();
        panel.hudHostModule().setSquaredCornerMask(RectCornerRoundMask.NONE);
    }

    /**
     * Whether this node produced a preview during the latest layout.
     *
     * @return true when the panel has something to draw
     */
    public boolean hasPreview() {
        return drawPreview != null;
    }

    /**
     * Panel edited by this node.
     *
     * @return the HUD panel
     */
    public HudPanel getPanel() {
        return panel;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
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
