package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.anim.FadeInAnim;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiScale;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.AlphaNode;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModSettingsScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.hud.HudManager;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudPanelMerger;
import cc.fascinated.fascinatedutils.systems.hud.node.HudLayerNode;
import cc.fascinated.fascinatedutils.systems.hud.node.HudPanelNode;
import cc.fascinated.fascinatedutils.updater.Updater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Full-screen HUD layout editor built from nodes: one {@link HudEditorPanelNode} per visible panel plus the branding,
 * snap guide and close chrome. The tree is recomposed every frame, so the panel previews are the same live node subtrees the game
 * draws (animation, merged corners and which panel owns the action buttons follow the frame clock and the pointer);
 * drag state lives in {@link HudEditorPointerSession}.
 */
public class HudEditorScreen extends RootScreen {

    private static final int SNAP_GUIDE_COLOR = UiColor.argb("#b3913de2");
    private static final int SNAP_GUIDE_CENTER_COLOR = UiColor.argb("#ffe0c84a");
    private static final int BRANDING_WIDTH = 280;
    private static final int BRANDING_PADDING = UiMetrics.SPACE_MD;
    private static final int BRANDING_GAP = UiMetrics.SPACE_SM;
    private static final int BRANDING_HEIGHT = 2 * BRANDING_PADDING + 10 + 10 + UiMetrics.CONTROL_HEIGHT + 2 * BRANDING_GAP;
    private static final int TITLE_HEIGHT = 10;
    private static final int MODS_BUTTON_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int CLOSE_EDITOR_SIZE = UiMetrics.CONTROL_HEIGHT;
    private static final int CORNER_MARGIN = 5;
    private static final int LABEL_HEIGHT = 10;

    private final HudEditorPointerSession pointerSession = new HudEditorPointerSession();
    private final HudPanelMerger panelMerger = new HudPanelMerger();
    private final FadeInAnim screenAnim = new FadeInAnim(200f);
    private final FadeInAnim brandingAnim = new FadeInAnim(450f);
    private float deltaSeconds;
    private HudPanel actionOwner;

    public HudEditorScreen() {
        super(Component.translatable("alumite.setting.hud_editor.title"));
    }

    @Override
    public void init() {
        super.init();
        HudManager.INSTANCE.markEditModeActive();
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        deltaSeconds = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks() / 20f;
        deltaSeconds = Mth.clamp(deltaSeconds, 0f, 1f);
        screenAnim.tick(deltaSeconds);
        screenAnim.show();
        brandingAnim.tick(deltaSeconds);
        if (!pointerSession.interacting() && screenAnim.progress().value() >= 0.80f) {
            brandingAnim.show();
        }
        else {
            brandingAnim.hide();
        }
        actionOwner = findActionOwner(HudEditorChrome.visibleLayoutWidgets(HudManager.INSTANCE.getWidgets()));
        super.renderCustom(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Picks the panel that shows the settings and close buttons: the selected panel while the pointer is in its action
     * zone, otherwise the topmost panel under the pointer's action zone, otherwise the selected panel.
     *
     * @param panels the visible panels in draw order
     * @return the owning panel, or null when there is none
     */
    private HudPanel findActionOwner(List<HudPanel> panels) {
        float pointerX = UiScale.uiPointerX();
        float pointerY = UiScale.uiPointerY();
        HudPanel selectedPanel = pointerSession.selected();
        boolean selectedVisible = selectedPanel != null && panels.contains(selectedPanel);
        if (selectedVisible && HudEditorChrome.isInActionZone(selectedPanel, pointerX, pointerY)) {
            return selectedPanel;
        }
        for (int index = panels.size() - 1; index >= 0; index--) {
            if (HudEditorChrome.isInActionZone(panels.get(index), pointerX, pointerY)) {
                return panels.get(index);
            }
        }
        return selectedVisible ? selectedPanel : null;
    }

    @Override
    protected boolean recomposesEveryFrame() {
        return true;
    }

    @Override
    protected boolean usesHudCanvas() {
        return true;
    }

    @Override
    protected UiNode composeContent() {
        int canvasWidth = Math.round(HudLayoutCanvas.width());
        int canvasHeight = Math.round(HudLayoutCanvas.height());
        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(new HudEditorBackdropNode(pointerSession));
        if (pointerSession.dragging() != null) {
            root.addChild(new HudEditorCrosshairNode());
        }

        List<HudPanel> panels = HudEditorChrome.visibleLayoutWidgets(HudManager.INSTANCE.getWidgets());
        HudLayerNode layer = new HudLayerNode(panelMerger);
        for (HudPanel panel : panels) {
            boolean repositionFromAnchor = panel != pointerSession.dragging() && panel != pointerSession.scalingWidget();
            HudPanelNode panelNode = panel.composeNode(deltaSeconds, true, repositionFromAnchor);
            layer.addChild(new HudEditorPanelNode(panel, panelNode, pointerSession, this, () -> actionOwner, panel == pointerSession.selected()).setNodeId("hud-panel-" + panel.getId()));
        }
        root.addChild(layer);

        addSnapGuides(root, canvasWidth, canvasHeight);
        root.addChild(composeBranding());
        root.addChild(composeCloseButton());
        root.addChild(composeVersionLabels());
        return root;
    }

    private void addSnapGuides(PositionedNode<?> root, int canvasWidth, int canvasHeight) {
        if (Float.isFinite(pointerSession.snapGuideX())) {
            int guideX = Math.round(Mth.clamp(pointerSession.snapGuideX(), 0f, canvasWidth));
            root.addChild(new RectNode()
                    .setFillArgb(pointerSession.snapGuideXIsCenter() ? SNAP_GUIDE_CENTER_COLOR : SNAP_GUIDE_COLOR)
                    .left(guideX).top(0).size(1, canvasHeight));
        }
        if (Float.isFinite(pointerSession.snapGuideY())) {
            int guideY = Math.round(Mth.clamp(pointerSession.snapGuideY(), 0f, canvasHeight));
            root.addChild(new RectNode()
                    .setFillArgb(pointerSession.snapGuideYIsCenter() ? SNAP_GUIDE_CENTER_COLOR : SNAP_GUIDE_COLOR)
                    .left(0).top(guideY).size(canvasWidth, 1));
        }
    }

    private UiNode composeBranding() {
        AlphaNode branding = new AlphaNode(() -> brandingAnim.progress().value());
        branding.size(BRANDING_WIDTH, BRANDING_HEIGHT).center();
        CardNode card = new CardNode().full();
        card.setFillResolver(UiTheme::panelFill);
        card.setBorderResolver(UiTheme::borderSubtle);
        card.setContents(contents -> {
            contents.margin(BRANDING_PADDING);
            contents.columnGap(BRANDING_GAP);
            contents.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.hud_editor.branding.title"))
                    .setBold(true)
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth()
                    .height(TITLE_HEIGHT));
            contents.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.hud_editor.branding.hint"))
                    .setColorResolver(UiTheme::textMuted)
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth()
                    .height(LABEL_HEIGHT));
            ButtonNode modsButton = new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.setting.hud_editor.branding.mods_button"))
                    .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                    .setOnPress(() -> Minecraft.getInstance().setScreen(new ModSettingsScreen(null, this)));
            modsButton.fullWidth().height(MODS_BUTTON_HEIGHT);
            contents.addChild(modsButton);
        });
        branding.addChild(card);
        return branding;
    }

    private UiNode composeCloseButton() {
        ButtonNode closeButton = new ButtonNode()
                .setIconCenter(ModUiTextures.CLOSE.getId())
                .setIconSize(16)
                .setOnPress(() -> Minecraft.getInstance().setScreen(null));
        closeButton.size(CLOSE_EDITOR_SIZE).right(CORNER_MARGIN).top(CORNER_MARGIN);
        return closeButton;
    }

    private UiNode composeVersionLabels() {
        boolean hasUpdate = Updater.INSTANCE.isHasUpdate();
        int lineCount = hasUpdate ? 2 : 1;
        PositionedNode<?> labels = new PositionedNode<>()
                .left(CORNER_MARGIN).bottom(CORNER_MARGIN)
                .size(BRANDING_WIDTH, LABEL_HEIGHT * lineCount)
                .columnGap(0);
        if (hasUpdate) {
            labels.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.hud_editor.branding.update_available"))
                    .setColorArgb(0xFF55FF55).setTextAlign(0f, 0.5f).fullWidth().height(LABEL_HEIGHT));
        }
        labels.addChild(new TextNode(() -> Component.translatable("alumite.setting.hud_editor.branding.version_label", Constants.GAME_VERSION, Constants.MOD_VERSION).getString())
                .setColorArgb(0x80FFFFFF).setTextAlign(0f, 0.5f).fullWidth().height(LABEL_HEIGHT));
        return labels;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() != GLFW.GLFW_KEY_ESCAPE && pointerSession.nudgeSelected(event.key(), event.modifiers())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void onEscapePressed() {
        HudManager.INSTANCE.setEditMode(false);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void removed() {
        pointerSession.clearSnapGuides();
        HudManager.INSTANCE.clearEditModeAfterEditorRemoved();
        super.removed();
    }
}
