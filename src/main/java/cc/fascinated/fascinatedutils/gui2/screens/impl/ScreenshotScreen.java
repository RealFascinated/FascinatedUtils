package cc.fascinated.fascinatedutils.gui2.screens.impl;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.gui2.core.*;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.ClickOverlayNode;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Paths;
import java.util.List;

public class ScreenshotScreen extends RootScreen {

    private static final int LABEL_HEIGHT = 18;
    private static final int BAR_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 18;
    private static final int PADDING = 8;
    private static final int GAP = 8;
    private static final int GRID_PADDING = 8;
    private static final int MIN_CELL_WIDTH = 240;
    private static final int TEXTURE_MAX_HEIGHT = 300;
    private static final int CELL_BTN_HEIGHT = 18;
    private static final int CELL_FOOTER_HEIGHT = LABEL_HEIGHT + 4;

    public ScreenshotScreen() {
        super(Component.translatable("alumite.screenshot.screen.title"));
    }

    @Override
    protected UiNode composeContent() {
        List<Screenshot> screenshots = ScreenshotManager.getScreenshots();
        UiState<Integer> confirmDeleteIndex = stateStore.state("confirmDeleteIndex", -1);
        UiState<Integer> sendToFriendIndex = stateStore.state("sendToFriendIndex", -1);

        RectNode background = new RectNode();
        background.full();
        background.setFillResolver(UiTheme::popupBackdropFill);

        PanelNode topBar = new PanelNode();
        topBar.fullWidth().top(0).height(BAR_HEIGHT);
        TextNode titleNode = new TextNode(() -> Component.translatable("alumite.screenshot.screen.title_fmt", NumberUtils.formatWithCommas(screenshots.size())).getString());
        titleNode.full();
        topBar.addChild(titleNode);

        UiState<Integer> listScrollState = stateStore.state("screenshot.list.scroll", 0);
        ScrollColumnNode scrollColumn = new ScrollColumnNode().bindScrollState(listScrollState);
        if (screenshots.isEmpty()) {
            TextNode emptyLabel = new TextNode(() -> Component.translatable("alumite.screenshot.screen.empty").getString());
            emptyLabel.full();
            scrollColumn.addChild(emptyLabel);
        } else {
            GridNode grid = new GridNode()
                    .setMinCellWidth(MIN_CELL_WIDTH)
                    .setCellAspectRatio(16f / 9f)
                    .setRowFooterHeight(CELL_FOOTER_HEIGHT)
                    .setGap(GAP);
            for (int i = 0; i < screenshots.size(); i++) {
                final int screenshotIndex = i;
                Screenshot screenshot = screenshots.get(i);

                // Buttons are hidden by default and shown on hover, overlaid on the thumbnail.
                // Defined before the cell so the hover callbacks can reference them.
                PositionedNode buttonRow = new PositionedNode();
                buttonRow.fullWidth().height(CELL_BTN_HEIGHT).bottom(CELL_FOOTER_HEIGHT + 4).left(4);
                buttonRow.setVisible(false);
                buttonRow.rowGap(4);

                ButtonNode copyBtn = new ButtonNode();
                copyBtn.size(BUTTON_HEIGHT, CELL_BTN_HEIGHT);
                copyBtn.setIconCenter(ModUiTextures.COPY.getId());
                copyBtn.setOnPress(screenshot::copyToClipboard);
                copyBtn.setRounded(true);
                buttonRow.addChild(copyBtn);

                ButtonNode sendBtn = new ButtonNode();
                sendBtn.size(BUTTON_HEIGHT, CELL_BTN_HEIGHT);
                sendBtn.setIconCenter(ModUiTextures.SHARE.getId());
                sendBtn.setOnPress(() -> sendToFriendIndex.set(screenshotIndex));
                sendBtn.setRounded(true);
                buttonRow.addChild(sendBtn);

                ButtonNode deleteBtn = new ButtonNode();
                deleteBtn.size(BUTTON_HEIGHT, CELL_BTN_HEIGHT);
                deleteBtn.setIconCenter(ModUiTextures.TRASH.getId());
                deleteBtn.setVariant(ButtonNode.ButtonVariant.DANGER);
                deleteBtn.setOnPress(() -> confirmDeleteIndex.set(screenshotIndex));
                deleteBtn.setRounded(true);
                buttonRow.addChild(deleteBtn);

                PositionedNode cell = new PositionedNode().full();
                cell.setOnPointerEnter(() -> buttonRow.setVisible(true));
                cell.setOnPointerLeave(() -> buttonRow.setVisible(false));

                TextureNode thumbnail = new TextureNode()
                        .setLoadedTextureSupplier(() -> screenshot.getTexture(TEXTURE_MAX_HEIGHT))
                        .setCornerRadius(3)
                        .setShowLoader(true);
                thumbnail.fullWidth().top(0).bottom(CELL_FOOTER_HEIGHT);
                cell.addChild(thumbnail);

                TextNode label = new TextNode(screenshot::getName);
                label.fullWidth().height(LABEL_HEIGHT).bottom(2);
                label.setTextAlign(0.5f, 1.0f);
                cell.addChild(label);

                // ClickOverlay added before buttonRow — buttons (added last) get hit priority.
                cell.addChild(new ClickOverlayNode(() ->
                        Minecraft.getInstance().setScreen(new ScreenshotViewerScreen(screenshotIndex))));
                cell.addChild(buttonRow);

                grid.addChild(cell);
            }
            scrollColumn.addChild(grid);
        }

        PanelNode bottomBar = new PanelNode();
        bottomBar.fullWidth().bottom(0).height(BAR_HEIGHT);

        ButtonNode openFolderButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.screen.open_folder").getString());
        openFolderButton.size(90, BUTTON_HEIGHT).left(PADDING).topRel(0.5f, 0, 0.5f);
        openFolderButton.setOnPress(() -> Util.getPlatform().openFile(
                Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "screenshots").toFile()));
        openFolderButton.setRounded(true);
        bottomBar.addChild(openFolderButton);

        ButtonNode closeButton = new ButtonNode();
        closeButton.size(BUTTON_HEIGHT, BUTTON_HEIGHT).right(6).alignY(0.5f);
        closeButton.setIconCenter(ModUiTextures.CLOSE.getId());
        closeButton.setOnPress(() -> Minecraft.getInstance().setScreen(null));
        closeButton.setRounded(true);
        bottomBar.addChild(closeButton);

        PositionedNode scrollArea = new PositionedNode();
        scrollArea.left(GRID_PADDING).right(GRID_PADDING).top(BAR_HEIGHT + GRID_PADDING).bottom(BAR_HEIGHT + GRID_PADDING);
        scrollArea.addChild(scrollColumn);

        StackNode root = new StackNode();
        root.addChild(background);
        root.addChild(topBar);
        root.addChild(scrollArea);
        root.addChild(bottomBar);

        int deleteIdx = confirmDeleteIndex.get();
        if (deleteIdx >= 0 && deleteIdx < screenshots.size()) {
            Screenshot toDelete = screenshots.get(deleteIdx);
            ConfirmPopupNode deletePopup = new ConfirmPopupNode();
            deletePopup.setTitle(Component.translatable("alumite.screenshot.delete_popup.title").getString());
            deletePopup.setMessage(Component.translatable("alumite.screenshot.delete_popup.message_fmt", toDelete.getName()).getString());
            deletePopup.setConfirmLabel(Component.translatable("alumite.screenshot.delete_popup.confirm").getString());
            deletePopup.setOnCancel(() -> confirmDeleteIndex.set(-1));
            deletePopup.setOnConfirm(() -> {
                confirmDeleteIndex.set(-1);
                ScreenshotManager.delete(toDelete);
            });
            root.addChild(deletePopup);
        }

        int sendIdx = sendToFriendIndex.get();
        if (sendIdx >= 0 && sendIdx < screenshots.size()) {
            SendScreenshotPopupNode sendPopup = new SendScreenshotPopupNode(
                    screenshots.get(sendIdx),
                    () -> sendToFriendIndex.set(-1));
            root.addChild(sendPopup);
        }

        return root;
    }

}
