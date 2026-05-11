package cc.fascinated.fascinatedutils.gui2.screens.impl;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui2.core.StackNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui2.node.PanelNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.TextureNode;
import cc.fascinated.fascinatedutils.gui2.node.screenshot.SendScreenshotPopupNode;
import cc.fascinated.fascinatedutils.gui2.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.screenshot.Screenshot;
import cc.fascinated.fascinatedutils.systems.screenshot.ScreenshotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ScreenshotViewerScreen extends RootScreen {

    private static final int BAR_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 18;
    private static final int PADDING = 24;
    private static final int TOP_HEIGHT = 42;
    private static final int NAV_BUTTON_WIDTH = 40;

    private final int initialIndex;
    private UiState<Integer> indexState;
    private UiState<Integer> listVersion;

    public ScreenshotViewerScreen(int initialIndex) {
        super(Component.translatable("alumite.screenshot.viewer.title"));
        this.initialIndex = initialIndex;
    }

    @Override
    protected UiNode composeContent() {
        indexState = stateStore.state("index", initialIndex);
        listVersion = stateStore.state("listVersion", 0);
        UiState<Boolean> confirmDelete = stateStore.state("confirmDelete", false);
        UiState<Boolean> showSendPopup = stateStore.state("showSendPopup", false);
        List<Screenshot> screenshots = ScreenshotManager.getScreenshots();

        if (screenshots.isEmpty()) {
            Minecraft.getInstance().setScreen(new ScreenshotScreen());
            return new PositionedNode().full();
        }

        int clamped = Math.clamp(indexState.get(), 0, screenshots.size() - 1);
        if (clamped != indexState.get()) {
            indexState.set(clamped);
        }

        RectNode background = new RectNode();
        background.full();
        background.setFillResolver(UiTheme::popupBackdropFill);

        // Top info: filename + counter
        PositionedNode topInfo = new PositionedNode();
        topInfo.fullWidth().top(0).height(TOP_HEIGHT);

        Screenshot screenshot = screenshots.get(indexState.get());
        TextNode nameLabel = new TextNode(screenshot::getName);
        nameLabel.fullWidth().top(7).height(12);
        nameLabel.setTextAlign(0.5f, 0.5f);
        topInfo.addChild(nameLabel);

        TextNode counterLabel = new TextNode(() -> (indexState.get() + 1) + " / " + screenshots.size());
        counterLabel.fullWidth().top(22).height(12);
        counterLabel.setTextAlign(0.5f, 0.5f);
        topInfo.addChild(counterLabel);

        // Screenshot image
        PositionedNode imageArea = new PositionedNode();
        imageArea.left(PADDING).right(PADDING).top(TOP_HEIGHT + PADDING).bottom(BAR_HEIGHT + PADDING);

        TextureNode image = new TextureNode()
                .setLoadedTextureSupplier(() -> screenshot.getTexture(0))
                .setCornerRadius(4)
                .setShowLoader(true)
                .naturalSize();
        image.full();
        imageArea.addChild(image);

        // Bottom bar
        PanelNode bottomBar = new PanelNode();
        bottomBar.fullWidth().bottom(0).height(BAR_HEIGHT);

        PositionedNode barRow = new PositionedNode();
        barRow.left(PADDING).right(PADDING).topRel(0.5f, 0, 0.5f).height(BUTTON_HEIGHT).rowGap(4);

        ButtonNode backButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.back").getString());
        backButton.height(BUTTON_HEIGHT);
        backButton.setLeftIcon(ModUiTextures.BACK.getId());
        backButton.setOnPress(() -> Minecraft.getInstance().setScreen(new ScreenshotScreen()));
        backButton.setRounded(true);
        barRow.addChild(backButton);

        ButtonNode copyButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.copy").getString());
        copyButton.height(BUTTON_HEIGHT);
        copyButton.setLeftIcon(ModUiTextures.COPY.getId());
        copyButton.setOnPress(screenshot::copyToClipboard);
        copyButton.setRounded(true);
        barRow.addChild(copyButton);

        ButtonNode deleteButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.delete").getString());
        deleteButton.height(BUTTON_HEIGHT);
        deleteButton.setLeftIcon(ModUiTextures.TRASH.getId());
        deleteButton.setVariant(ButtonNode.ButtonVariant.DANGER);
        deleteButton.setOnPress(() -> confirmDelete.set(true));
        deleteButton.setRounded(true);
        barRow.addChild(deleteButton);

        ButtonNode sendButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.send_to_friend").getString());
        sendButton.height(BUTTON_HEIGHT);
        sendButton.setOnPress(() -> showSendPopup.set(true));
        sendButton.setLeftIcon(ModUiTextures.SHARE.getId());
        sendButton.setRounded(true);
        barRow.addChild(sendButton);

        barRow.addChild(new SpacerNode());

        ButtonNode prevButton = new ButtonNode();
        prevButton.height(BUTTON_HEIGHT).width(NAV_BUTTON_WIDTH);
        prevButton.setIconCenter(ModUiTextures.CHEVRON_LEFT.getId());
        prevButton.setOnPress(() -> navigate(-1));
        prevButton.setRounded(true);
        barRow.addChild(prevButton);

        ButtonNode nextButton = new ButtonNode();
        nextButton.height(BUTTON_HEIGHT).width(NAV_BUTTON_WIDTH);
        nextButton.setIconCenter(ModUiTextures.CHEVRON_RIGHT.getId());
        nextButton.setOnPress(() -> navigate(1));
        nextButton.setRounded(true);
        barRow.addChild(nextButton);

        bottomBar.addChild(barRow);

        StackNode root = new StackNode();
        root.addChild(background);
        root.addChild(topInfo);
        root.addChild(imageArea);
        root.addChild(bottomBar);

        if (confirmDelete.get()) {
            ConfirmPopupNode popup = new ConfirmPopupNode();
            popup.setTitle(Component.translatable("alumite.screenshot.delete_popup.title").getString());
            popup.setMessage(Component.translatable("alumite.screenshot.delete_popup.message").getString());
            popup.setConfirmLabel(Component.translatable("alumite.screenshot.delete_popup.confirm").getString());
            popup.setOnCancel(() -> confirmDelete.set(false));
            popup.setOnConfirm(() -> {
                confirmDelete.set(false);
                deleteScreenshot(screenshots, indexState.get());
            });
            root.addChild(popup);
        }

        if (showSendPopup.get()) {
            SendScreenshotPopupNode sendPopup = new SendScreenshotPopupNode(
                    screenshot,
                    () -> showSendPopup.set(false));
            root.addChild(sendPopup);
        }

        return root;
    }

    private void navigate(int delta) {
        if (indexState == null) {
            return;
        }

        int next = indexState.get() + delta;
        int size = ScreenshotManager.getScreenshots().size();
        if (next >= 0 && next < size) {
            indexState.set(next);
        }
    }

    private void deleteScreenshot(List<Screenshot> screenshots, int index) {
        if (index < 0 || index >= screenshots.size()) return;
        Screenshot screenshot = screenshots.get(index);
        ScreenshotManager.delete(screenshot);
        if (ScreenshotManager.getScreenshots().isEmpty()) {
            Minecraft.getInstance().setScreen(new ScreenshotScreen());
        } else if (indexState != null && listVersion != null) {
            int nextIndex = Math.min(index, ScreenshotManager.getScreenshots().size() - 1);
            listVersion.set(listVersion.get() + 1);
            indexState.set(nextIndex);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            navigate(-1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            navigate(1);
            return true;
        }
        return super.keyPressed(event);
    }
}
