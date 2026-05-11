package cc.fascinated.fascinatedutils.gui2.screens.impl;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.*;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.render.GuiRenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.oldgui.screens.HUDEditorScreen;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ActionsOverlayScreen {

    public static final ActionsOverlayScreen INSTANCE = new ActionsOverlayScreen();

    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int RIGHT_MARGIN = 8;
    private static final int LOGIN_TEXT_X = 5;
    private static final int LOGIN_TEXT_Y = 5;

    private final UiHost host = new UiHost();

    private static final List<Button> BUTTONS = List.of(
            new Button(
                    "hud-editor",
                    "Hud Editor",
                    ModUiTextures.SETTINGS.getId(),
                    () -> Minecraft.getInstance().setScreen(new HUDEditorScreen())
            ),
            new Button(
                    "social",
                    "Social",
                    ModUiTextures.GROUP.getId(),
                    () -> Minecraft.getInstance().setScreen(new SocialScreen())
            ),
            new Button(
                    "screenshots",
                    "Screenshots",
                    ModUiTextures.IMAGE.getId(),
                    () -> Minecraft.getInstance().setScreen(new ScreenshotScreen())
            )
    );

    private ActionsOverlayScreen() {
        host.setComposer(_ -> composeRoot());
    }

    private UiNode composeRoot() {
        PositionedNode rootNode = new PositionedNode().full();
        rootNode.setNodeId("title-root");

        PositionedNode buttonGroup = new PositionedNode()
                .size(BUTTON_WIDTH, BUTTON_HEIGHT * BUTTONS.size() + BUTTON_GAP)
                .right(RIGHT_MARGIN).alignY(0.5f)
                .columnGap(BUTTON_GAP);

        for (Button button : BUTTONS) {
            ButtonNode node = new ButtonNode(button.displayName());
            node.setNodeId(button.id());
            node.size(BUTTON_WIDTH, BUTTON_HEIGHT);
            node.setRightIcon(button.icon());
            node.setOnPress(button.onClick());
            buttonGroup.addChild(node);
        }

        rootNode.addChild(loggedInAccountNode());
        rootNode.addChild(buttonGroup);
        return rootNode;
    }

    private static @NonNull TextNode loggedInAccountNode() {
        TextNode loginTextNode = new TextNode(() -> {
            var selfUser = Alumite.INSTANCE.users().selfUser();
            if (selfUser == null) {
                return "";
            }
            String minecraftName = selfUser.user().minecraftName();
            if (minecraftName == null || minecraftName.isBlank()) {
                return "";
            }
            return String.format(Component.translatable("alumite.title_screen.logged_in_as").getString(), minecraftName);
        });
        loginTextNode.setNodeId("alumite-username");
        loginTextNode.pos(LOGIN_TEXT_X, LOGIN_TEXT_Y);
        loginTextNode.setTextAlign(0f, 0f).setShadow(true);
        return loginTextNode;
    }

    public void render(GuiGraphicsExtractor graphics) {
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();

        UiTheme uiTheme = UiThemeRepository.get();
        GuiRenderFrame renderFrame = new GuiRenderFrame(graphics, FascinatedGuiTheme.INSTANCE, uiTheme);
        renderFrame.beginFrame(uiWidth, uiHeight);
        renderFrame.setPointer(pointerX, pointerY);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderFrame);
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        host.render(renderFrame, 0f);
        renderFrame.endFrame();
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        boolean hitRegion = GuiRenderer.recordPressedRegion(pointerX, pointerY, event.button());
        boolean pressed = hitRegion || host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
        host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
        GuiRenderer.fireAndClearPressedRegion(pointerX, pointerY);
        return pressed;
    }

    record Button(String id, String displayName, Identifier icon, Runnable onClick) {}
}
