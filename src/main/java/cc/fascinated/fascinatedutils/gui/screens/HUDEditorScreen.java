package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.common.ClientGuiUtils;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorAppearancePanelController;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorChrome;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorOverlays;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorPointerSession;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class HUDEditorScreen extends WidgetScreen {

    private final HudEditorAppearancePanelController appearancePanel = new HudEditorAppearancePanelController();
    private final HudEditorPointerSession pointerSession = new HudEditorPointerSession(appearancePanel);

    public HUDEditorScreen() {
        super(Component.translatable("fascinatedutils.setting.hud_editor.title"));
    }

    public boolean shouldPause() {
        return false;
    }

    public void renderBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ClientGuiUtils.unscaledProjection();
        try {
            float canvasWidth = HudEditorChrome.clampCanvasExtent(UIScale.logicalWidth());
            float canvasHeight = HudEditorChrome.clampCanvasExtent(UIScale.logicalHeight());
            GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
            guiRenderer.begin(canvasWidth, canvasHeight);
            float deltaSeconds = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks() / 20f;
            if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
                deltaSeconds = partialTick / 20f;
            }
            deltaSeconds = Mth.clamp(deltaSeconds, 0f, 1f);
            appearancePanel.clearBoundsForFrame();

            guiRenderer.drawRect(0f, 0f, canvasWidth, canvasHeight, UiColor.argb("#66000000"));

            List<HudModule> widgetList = HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets());
            HudModule dragging = pointerSession.dragging();
            HudModule scalingWidget = pointerSession.scalingWidget();
            HudModule selected = pointerSession.selected();
            for (HudModule widget : widgetList) {
                boolean reposition = widget != dragging && widget != scalingWidget;
                HudEditorChrome.drawWidgetEditorChrome(guiRenderer, widget, selected, deltaSeconds, canvasWidth, canvasHeight, reposition);
            }
            HudEditorOverlays.drawSnapGuides(guiRenderer, canvasWidth, canvasHeight, pointerSession.snapGuideX(), pointerSession.snapGuideY());
            boolean appearanceEligible = selected != null && widgetList.contains(selected) && dragging == null && scalingWidget == null && pointerSession.appearancePanelUnblocked();
            if (!appearanceEligible) {
                appearancePanel.clearPanel();
            }
            else {
                appearancePanel.layoutAndDraw(guiRenderer, selected, canvasWidth, canvasHeight, deltaSeconds);
            }
            if (pointerSession.showControlsHint()) {
                HudEditorOverlays.drawControlsHint(guiRenderer, canvasWidth, canvasHeight);
            }
            appearancePanel.dispatchMouseMoveLogical();
            guiRenderer.end();
        } finally {
            ClientGuiUtils.scaledProjection();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return pointerSession.onMouseClicked(event, doubled, () -> super.mouseClicked(event, doubled));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return pointerSession.onMouseDragged(event, dragX, dragY, () -> super.mouseDragged(event, dragX, dragY));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return pointerSession.onMouseReleased(event, () -> super.mouseReleased(event));
    }

    @Override
    public void mouseMoved(double mousePosX, double mousePosY) {
        pointerSession.onMouseMovedLogical();
        super.mouseMoved(mousePosX, mousePosY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return pointerSession.onMouseScrolled(verticalAmount, () -> super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return pointerSession.onKeyPressed(event, () -> super.keyPressed(event));
    }

    @Override
    public void removed() {
        pointerSession.clearSnapGuides();
        appearancePanel.dispose();
        HUDManager.INSTANCE.clearEditModeAfterEditorRemoved();
        super.removed();
    }
}
