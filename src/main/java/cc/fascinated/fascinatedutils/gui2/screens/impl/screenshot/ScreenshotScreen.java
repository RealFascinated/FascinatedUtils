package cc.fascinated.fascinatedutils.gui2.screens.impl.screenshot;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.CardNode;
import cc.fascinated.fascinatedutils.gui2.node.ClickOverlayNode;
import cc.fascinated.fascinatedutils.gui2.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui2.node.GridNode;
import cc.fascinated.fascinatedutils.gui2.node.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.TextureNode;
import cc.fascinated.fascinatedutils.gui2.node.screenshot.SendScreenshotPopupNode;
import cc.fascinated.fascinatedutils.gui2.screens.RootScreen;
import cc.fascinated.fascinatedutils.systems.screenshot.Screenshot;
import cc.fascinated.fascinatedutils.systems.screenshot.ScreenshotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Paths;
import java.util.List;

public class ScreenshotScreen extends RootScreen {

    private static final int LABEL_HEIGHT = 18;
    private static final int BUTTON_SIZE = 18;
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
        Minecraft mc = Minecraft.getInstance();
        List<Screenshot> screenshots = ScreenshotManager.getScreenshots();
        UiState<Integer> confirmDeleteIndex = stateStore.state("confirmDeleteIndex", -1);
        UiState<Integer> sendToFriendIndex = stateStore.state("sendToFriendIndex", -1);

        CardNode card = new CardNode()
                .setRounded(false)
                .full()
                .setHeader(header -> {
                    header.addChild(new TextNode(() -> Component.translatable("alumite.screenshot.screen.title_fmt", NumberUtils.formatWithCommas(screenshots.size())).getString()));

                    header.addChild(new SpacerNode());

                    header.right(8);
                    header.addChild(new ButtonNode()
                            .setLabel(Component.translatable("alumite.screenshot.screen.open_folder").getString())
                            .setRounded(true)
                            .setOnPress(() -> Util.getPlatform().openFile(Paths.get(mc.gameDirectory.getAbsolutePath(), "screenshots").toFile())));
                    header.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.CLOSE.getId())
                            .setRounded(true)
                            .setOnPress(() -> mc.setScreen(null)));
                })
                .setContents(contents -> {
                    contents.margin(GRID_PADDING);
                    ScrollColumnNode scrollColumn = new ScrollColumnNode().persistScroll("screenshot.list");
                    if (screenshots.isEmpty()) {
                        scrollColumn.addChild(new TextNode(() -> Component.translatable("alumite.screenshot.screen.empty").getString()).full());
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
                            // Defined before the texture so the hover callbacks can reference them.
                            PositionedNode<?> buttonRow = new PositionedNode<>()
                                    .fullWidth().height(CELL_BTN_HEIGHT).bottom(4).left(4)
                                    .rowGap(4);
                            buttonRow.setVisible(false);

                            buttonRow.addChild(new ButtonNode()
                                    .size(BUTTON_SIZE, CELL_BTN_HEIGHT)
                                    .setIconCenter(ModUiTextures.COPY.getId())
                                    .setOnPress(screenshot::copyToClipboard)
                                    .setRounded(true));

                            buttonRow.addChild(new ButtonNode()
                                    .size(BUTTON_SIZE, CELL_BTN_HEIGHT)
                                    .setIconCenter(ModUiTextures.SHARE.getId())
                                    .setOnPress(() -> sendToFriendIndex.set(screenshotIndex))
                                    .setRounded(true));

                            buttonRow.addChild(new ButtonNode()
                                    .size(BUTTON_SIZE, CELL_BTN_HEIGHT)
                                    .setIconCenter(ModUiTextures.TRASH.getId())
                                    .setVariant(ButtonNode.ButtonVariant.DANGER)
                                    .setOnPress(() -> confirmDeleteIndex.set(screenshotIndex))
                                    .setRounded(true));

                            TextureNode texture = new TextureNode()
                                    .setLoadedTextureSupplier(() -> screenshot.getTexture(TEXTURE_MAX_HEIGHT))
                                    .setCornerRadius(3)
                                    .setShowLoader(true)
                                    .fullWidth().top(0).bottom(CELL_FOOTER_HEIGHT);
                            texture.addChild(new ClickOverlayNode(() -> Minecraft.getInstance().setScreen(new ScreenshotViewerScreen(screenshotIndex))).full());
                            texture.addChild(buttonRow);

                            PositionedNode<?> cell = new PositionedNode<>().full();
                            cell.setOnPointerEnter(() -> buttonRow.setVisible(true));
                            cell.setOnPointerLeave(() -> buttonRow.setVisible(false));

                            cell.addChild(texture);

                            cell.addChild(new TextNode(screenshot::getName)
                                    .fullWidth().height(LABEL_HEIGHT).bottom(4)
                                    .setTextAlign(0.5f, 1.0f));

                            grid.addChild(cell);
                        }
                        scrollColumn.addChild(grid);
                    }
                    contents.addChild(scrollColumn);
                });

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(card);

        int deleteIdx = confirmDeleteIndex.get();
        if (deleteIdx >= 0 && deleteIdx < screenshots.size()) {
            Screenshot toDelete = screenshots.get(deleteIdx);
            root.addChild(new ConfirmPopupNode()
                    .setTitle(Component.translatable("alumite.screenshot.delete_popup.title").getString())
                    .setDescription(Component.translatable("alumite.screenshot.delete_popup.message_fmt", toDelete.getName()).getString())
                    .setConfirmLabel(Component.translatable("alumite.screenshot.delete_popup.confirm").getString())
                    .setOnCancel(() -> confirmDeleteIndex.set(-1))
                    .setOnConfirm(() -> {
                        confirmDeleteIndex.set(-1);
                        ScreenshotManager.delete(toDelete);
                    }));
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
