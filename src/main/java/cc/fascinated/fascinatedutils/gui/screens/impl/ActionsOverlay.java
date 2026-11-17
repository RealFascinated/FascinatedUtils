package cc.fascinated.fascinatedutils.gui.screens.impl;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.components.PanelCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.input.UiEvent;
import cc.fascinated.fascinatedutils.gui.core.UiHost;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.UiScale;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor.HudEditorScreen;
import cc.fascinated.fascinatedutils.gui.screens.impl.screenshot.ScreenshotScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Button panel drawn over the pause menu that opens the HUD editor and the screenshot browser. It is not a screen: the
 * pause menu forwards its render and mouse events here.
 */
public class ActionsOverlay {

    private static final int BUTTON_WIDTH = 112;
    private static final int BUTTON_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int PANEL_PADDING = 3;
    private static final int BUTTON_GAP = 2;
    private static final int RIGHT_MARGIN = 6;

    private static final List<Action> ACTIONS = List.of(
            new Action(
                    "hud-editor",
                    "alumite.actions.hud_editor",
                    ModUiTextures.SETTINGS.getId(),
                    () -> Minecraft.getInstance().setScreen(new HudEditorScreen())
            ),
            new Action(
                    "screenshots",
                    "alumite.actions.screenshots",
                    ModUiTextures.IMAGE.getId(),
                    () -> Minecraft.getInstance().setScreen(new ScreenshotScreen())
            )
    );

    public static final ActionsOverlay INSTANCE = new ActionsOverlay();

    private final UiHost host = new UiHost();
    private AlumiteGuiRenderer renderer;
    private long lastRenderNanos;

    private ActionsOverlay() {
        host.setComposer(_ -> composeRoot());
    }

    private UiNode composeRoot() {
        PositionedNode<?> rootNode = new PositionedNode<>().full();
        rootNode.setNodeId("title-root");

        int groupHeight = BUTTON_HEIGHT * ACTIONS.size() + BUTTON_GAP * (ACTIONS.size() - 1) + 2 * PANEL_PADDING;
        CardNode panel = new PanelCard()
                .size(BUTTON_WIDTH + 2 * PANEL_PADDING, groupHeight)
                .right(RIGHT_MARGIN).alignY(0.5f);
        panel.setContents(group -> {
            group.margin(PANEL_PADDING);
            group.columnGap(BUTTON_GAP);
            for (Action action : ACTIONS) {
                ButtonNode node = new ButtonNode(() -> UiTranslations.INSTANCE.get(action.translationKey()));
                node.setNodeId(action.id());
                node.size(BUTTON_WIDTH, BUTTON_HEIGHT);
                node.setLeftIcon(action.icon());
                node.setLeftAlignLabel(true);
                node.setVariant(ButtonNode.ButtonVariant.GHOST);
                node.setOnPress(action.onClick());
                group.addChild(node);
            }
        });
        rootNode.addChild(panel);
        return rootNode;
    }

    /**
     * Lays out, hover-tracks and draws the overlay for one frame.
     *
     * @param graphics the graphics extractor of the pause screen being drawn
     */
    public void render(GuiGraphicsExtractor graphics) {
        float uiWidth = UiScale.screenWidth();
        float uiHeight = UiScale.screenHeight();
        float pointerX = UiScale.screenPointerX();
        float pointerY = UiScale.screenPointerY();

        if (renderer == null) {
            renderer = new AlumiteGuiRenderer(graphics, true);
        }
        else {
            renderer.rebind(graphics, true);
        }
        renderer.beginFrame(uiWidth, uiHeight);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderer);
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        long now = System.nanoTime();
        float deltaSeconds = lastRenderNanos == 0L ? 0f : Math.min((now - lastRenderNanos) / 1_000_000_000f, 0.1f);
        lastRenderNanos = now;
        host.render(renderer, deltaSeconds);
        renderer.endFrame();
    }

    /**
     * Presses the button under the pointer.
     *
     * @param event the mouse press
     * @return true when the overlay consumed the press
     */
    public boolean mouseClicked(MouseButtonEvent event) {
        float pointerX = UiScale.screenPointerX();
        float pointerY = UiScale.screenPointerY();
        return host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
    }

    /**
     * Releases the pressed button, which activates it when the pointer is still over it.
     *
     * @param event the mouse release
     * @return true when the overlay consumed the release
     */
    public boolean mouseReleased(MouseButtonEvent event) {
        float pointerX = UiScale.screenPointerX();
        float pointerY = UiScale.screenPointerY();
        return host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
    }

    private record Action(String id, String translationKey, Identifier icon, Runnable onClick) {}
}
