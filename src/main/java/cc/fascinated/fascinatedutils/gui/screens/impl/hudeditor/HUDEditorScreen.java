package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.anim.FadeInAnim;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UIScale;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.AlphaNode;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.RectNode;
import cc.fascinated.fascinatedutils.gui.node.TextNode;
import cc.fascinated.fascinatedutils.gui.node.CardNode;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.settings.ModSettingsScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.updater.Updater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Full-screen HUD layout editor built from nodes: one {@link HudPanelNode} per visible panel plus the branding,
 * snap guide and close chrome. The tree is rebuilt every frame; drag state lives in {@link HudEditorPointerSession}.
 */
public class HUDEditorScreen extends RootScreen {

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
    private final FadeInAnim screenAnim = new FadeInAnim(200f);
    private final FadeInAnim brandingAnim = new FadeInAnim(450f);
    private float deltaSeconds;

    public HUDEditorScreen() {
        super(Component.translatable("alumite.setting.hud_editor.title"));
    }

    @Override
    public void init() {
        super.init();
        HUDManager.INSTANCE.markEditModeActive();
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        deltaSeconds = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = partialTick / 20f;
        }
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
        super.renderCustom(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean usesHudCanvas() {
        return true;
    }

    @Override
    protected UiNode composeContent() {
        int canvasWidth = Math.round(HudLayoutCanvas.width());
        int canvasHeight = Math.round(HudLayoutCanvas.height());
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(new HudEditorBackdropNode(pointerSession));
        if (pointerSession.dragging() != null) {
            root.addChild(new HudEditorCrosshairNode());
        }

        List<HudPanel> panels = HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets());
        HudPanel hoverOwner = null;
        for (int index = panels.size() - 1; index >= 0; index--) {
            if (HudEditorChrome.isInActionZone(panels.get(index), pointerX, pointerY)) {
                hoverOwner = panels.get(index);
                break;
            }
        }
        for (HudPanel panel : panels) {
            boolean repositionFromAnchor = panel != pointerSession.dragging() && panel != pointerSession.scalingWidget();
            root.addChild(new HudPanelNode(panel, pointerSession, this, deltaSeconds, repositionFromAnchor, panel == hoverOwner, panel == pointerSession.selected()));
        }

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
        branding.setVisible(brandingAnim.isVisible());
        branding.size(BRANDING_WIDTH, BRANDING_HEIGHT).center();
        CardNode card = new CardNode().full();
        card.setFillResolver(UiTheme::panelFill);
        card.setBorderResolver(UiTheme::borderSubtle);
        card.setContents(contents -> {
            contents.margin(BRANDING_PADDING);
            contents.columnGap(BRANDING_GAP);
            contents.addChild(new TextNode(() -> Component.translatable("alumite.setting.hud_editor.branding.title").getString())
                    .setBold(true)
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth()
                    .height(TITLE_HEIGHT));
            contents.addChild(new TextNode(() -> Component.translatable("alumite.setting.hud_editor.branding.hint").getString())
                    .setColorResolver(UiTheme::textMuted)
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth()
                    .height(LABEL_HEIGHT));
            ButtonNode modsButton = new ButtonNode(() -> Component.translatable("alumite.setting.hud_editor.branding.mods_button").getString())
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
            labels.addChild(new TextNode(() -> Component.translatable("alumite.setting.hud_editor.branding.update_available").getString())
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
        HUDManager.INSTANCE.setEditMode(false);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void removed() {
        pointerSession.clearSnapGuides();
        HUDManager.INSTANCE.clearEditModeAfterEditorRemoved();
        super.removed();
    }
}
