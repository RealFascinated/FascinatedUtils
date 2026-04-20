package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorChrome;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorOverlays;
import cc.fascinated.fascinatedutils.gui.hudeditor.HudEditorPointerSession;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class HUDEditorScreen extends WidgetScreen {

    private final HudEditorPointerSession pointerSession = new HudEditorPointerSession();

    public HUDEditorScreen() {
        super(Component.translatable("fascinatedutils.setting.hud_editor.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void renderBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        pointerSession.syncEditorCanvas(canvasWidth, canvasHeight);
        GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        guiRenderer.begin(canvasWidth, canvasHeight);
        HudEditorOverlays.clearBrandingHitLayout();
        float deltaSeconds = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = partialTick / 20f;
        }
        deltaSeconds = Mth.clamp(deltaSeconds, 0f, 1f);
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
        boolean idleHudSelection = pointerSession.selected() == null && pointerSession.dragging() == null && pointerSession.scalingWidget() == null;
        if (idleHudSelection) {
            HudEditorOverlays.drawBrandingCenterOverlay(guiRenderer, canvasWidth, canvasHeight, UIScale.uiPointerX(), UIScale.uiPointerY());
        }
        guiRenderer.end();
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
        HUDManager.INSTANCE.clearEditModeAfterEditorRemoved();
        super.removed();
    }
}
