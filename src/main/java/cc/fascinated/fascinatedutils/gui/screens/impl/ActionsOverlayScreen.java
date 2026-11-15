package cc.fascinated.fascinatedutils.gui.screens.impl;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.CardNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.impl.screenshot.ScreenshotScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor.HUDEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ActionsOverlayScreen {

    public static final ActionsOverlayScreen INSTANCE = new ActionsOverlayScreen();

    private static final int BUTTON_WIDTH = 112;
    private static final int BUTTON_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int PANEL_PADDING = 3;
    private static final int BUTTON_GAP = 2;
    private static final int RIGHT_MARGIN = 6;

    private final UiHost host = new UiHost();
    private long lastRenderNanos;

    private static final List<Button> BUTTONS = List.of(
            new Button(
                    "hud-editor",
                    "alumite.actions.hud_editor",
                    ModUiTextures.SETTINGS.getId(),
                    () -> Minecraft.getInstance().setScreen(new HUDEditorScreen())
            ),
            new Button(
                    "screenshots",
                    "alumite.actions.screenshots",
                    ModUiTextures.IMAGE.getId(),
                    () -> Minecraft.getInstance().setScreen(new ScreenshotScreen())
            )
    );

    private ActionsOverlayScreen() {
        host.setComposer(_ -> composeRoot());
    }

    private UiNode composeRoot() {
        PositionedNode<?> rootNode = new PositionedNode<>().full();
        rootNode.setNodeId("title-root");

        int groupHeight = BUTTON_HEIGHT * BUTTONS.size() + BUTTON_GAP * (BUTTONS.size() - 1) + 2 * PANEL_PADDING;
        CardNode panel = new CardNode()
                .size(BUTTON_WIDTH + 2 * PANEL_PADDING, groupHeight)
                .right(RIGHT_MARGIN).alignY(0.5f);
        panel.setFillResolver(UiTheme::panelFill);
        panel.setBorderResolver(UiTheme::borderSubtle);
        panel.setContents(group -> {
            group.margin(PANEL_PADDING);
            group.columnGap(BUTTON_GAP);
            for (Button button : BUTTONS) {
                ButtonNode node = new ButtonNode(() -> Component.translatable(button.translationKey()).getString());
                node.setNodeId(button.id());
                node.size(BUTTON_WIDTH, BUTTON_HEIGHT);
                node.setLeftIcon(button.icon());
                node.setLeftAlignLabel(true);
                node.setVariant(ButtonNode.ButtonVariant.GHOST);
                node.setOnPress(button.onClick());
                group.addChild(node);
            }
        });
        rootNode.addChild(panel);
        return rootNode;
    }

    public void render(GuiGraphicsExtractor graphics) {
        float uiWidth = UIScale.screenWidth();
        float uiHeight = UIScale.screenHeight();
        float pointerX = UIScale.screenPointerX();
        float pointerY = UIScale.screenPointerY();

        GuiRenderer renderFrame = new GuiRenderer(graphics);
        renderFrame.beginFrame(uiWidth, uiHeight);
        renderFrame.setPointer(pointerX, pointerY);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderFrame);
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        long now = System.nanoTime();
        float deltaSeconds = lastRenderNanos == 0L ? 0f : Math.min((now - lastRenderNanos) / 1_000_000_000f, 0.1f);
        lastRenderNanos = now;
        host.render(renderFrame, deltaSeconds);
        renderFrame.endFrame();
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        float pointerX = UIScale.screenPointerX();
        float pointerY = UIScale.screenPointerY();
        boolean hitRegion = GuiRenderer.recordPressedRegion(pointerX, pointerY, event.button());
        boolean pressed = hitRegion || host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
        host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
        GuiRenderer.fireAndClearPressedRegion(pointerX, pointerY);
        return pressed;
    }

    record Button(String id, String translationKey, Identifier icon, Runnable onClick) {}
}
