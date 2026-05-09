package cc.fascinated.fascinatedutils.gui2.screens.impl;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UIScale;
import cc.fascinated.fascinatedutils.gui2.core.UiEvent;
import cc.fascinated.fascinatedutils.gui2.core.UiHost;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.render.GuiRenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.oldgui.screens.HUDEditorScreen;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class ActionsOverlayScreen {

    public static final ActionsOverlayScreen INSTANCE = new ActionsOverlayScreen();

    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int RIGHT_MARGIN = 8;
    private static final int GROUP_TOP_OFFSET = -23;
    private static final int LOGIN_TEXT_X = 5;
    private static final int LOGIN_TEXT_Y = 5;

    private final UiHost host = new UiHost();

    private ActionsOverlayScreen() {
        host.setComposer(unusedStateStore -> composeRoot());
    }

    private UiNode composeRoot() {
        PositionedNode rootNode = new PositionedNode().full();
        rootNode.setNodeId("title-root");

        ButtonNode settingsButton = new ButtonNode("HUD Editor");
        settingsButton.setNodeId("title-hud-editor");
        settingsButton.size(BUTTON_WIDTH, BUTTON_HEIGHT);
        settingsButton.right(RIGHT_MARGIN).topRel(0.5f, GROUP_TOP_OFFSET, 0f);
        settingsButton.setLeftIcon(ModUiTextures.SETTINGS.getId());
        settingsButton.setOnPress(() -> Minecraft.getInstance().setScreen(new HUDEditorScreen()));

        ButtonNode socialButton = new ButtonNode("Social");
        socialButton.setNodeId("title-social");
        socialButton.size(BUTTON_WIDTH, BUTTON_HEIGHT);
        socialButton.right(RIGHT_MARGIN).topRel(0.5f, GROUP_TOP_OFFSET + BUTTON_HEIGHT + BUTTON_GAP, 0f);
        socialButton.setLeftIcon(ModUiTextures.GROUP.getId());
        socialButton.setOnPress(() -> Minecraft.getInstance().setScreen(new SocialScreen()));

        TextNode loginTextNode = new TextNode(() -> {
            var selfUser = Alumite.INSTANCE.users().selfUser();
            if (selfUser == null) {
                return "";
            }
            String minecraftName = selfUser.user().minecraftName();
            if (minecraftName == null || minecraftName.isBlank()) {
                return "";
            }
            return "Alumite logged in as " + minecraftName;
        });
        loginTextNode.setNodeId("alumite-username");
        loginTextNode.pos(LOGIN_TEXT_X, LOGIN_TEXT_Y);
        loginTextNode.setTextAlign(0f, 0f).setShadow(true);

        rootNode.addChild(loginTextNode);
        rootNode.addChild(settingsButton);
        rootNode.addChild(socialButton);
        return rootNode;
    }

    public void render(GuiGraphicsExtractor graphics) {
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();

        UiTheme uiTheme = UiThemeRepository.get();
        GuiRenderFrame renderFrame = new GuiRenderFrame(graphics, FascinatedGuiTheme.INSTANCE, uiTheme);
        renderFrame.beginFrame(uiWidth, uiHeight);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderFrame);
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        host.render(renderFrame, 0f);
        renderFrame.endFrame();
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        boolean pressed = host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
        host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
        return pressed;
    }
}
