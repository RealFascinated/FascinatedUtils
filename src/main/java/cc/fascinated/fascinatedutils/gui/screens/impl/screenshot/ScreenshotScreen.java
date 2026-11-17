package cc.fascinated.fascinatedutils.gui.screens.impl.screenshot;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.util.NumberUtils;
import cc.fascinated.fascinatedutils.gui.components.CloseButton;
import cc.fascinated.fascinatedutils.gui.components.PanelCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.input.UiKeys;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.control.ClickOverlayNode;
import cc.fascinated.fascinatedutils.gui.node.popup.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.layout.GridNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextureNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
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

        CardNode card = new PanelCard()
                .setTitleHeader(() -> Component.translatable("alumite.screenshot.screen.title_fmt", NumberUtils.formatWithCommas(screenshots.size())).getString(),
                        header -> {
                            header.addChild(new ButtonNode()
                                    .setLabel(UiTranslations.INSTANCE.get("alumite.screenshot.screen.open_folder"))
                                    .setLeftIcon(ModUiTextures.IMAGE.getId())
                                    .setOnPress(() -> Util.getPlatform().openFile(Paths.get(mc.gameDirectory.getAbsolutePath(), "screenshots").toFile())));
                            header.addChild(new CloseButton(() -> mc.setScreen(null)));
                        })
                .full()
                .setContents(contents -> {
                    contents.margin(GRID_PADDING);
                    ScrollColumnNode scrollColumn = new ScrollColumnNode().persistScroll("screenshot.list");
                    if (screenshots.isEmpty()) {
                        scrollColumn.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.screenshot.screen.empty")).full());
                    } else {
                        GridNode grid = new GridNode()
                                .setMinCellWidth(MIN_CELL_WIDTH)
                                .setCellAspectRatio(16f / 9f)
                                .setRowFooterHeight(CELL_FOOTER_HEIGHT)
                                .setGap(GAP);
                        for (int screenshotIndex = 0; screenshotIndex < screenshots.size(); screenshotIndex++) {
                            final int viewerIndex = screenshotIndex;
                            Screenshot screenshot = screenshots.get(screenshotIndex);

                            // Buttons are hidden by default and shown on hover or keyboard focus, overlaid on the thumbnail.
                            PositionedNode<?> buttonRow = new PositionedNode<>()
                                    .fullWidth().height(CELL_BTN_HEIGHT).bottom(4).left(4)
                                    .rowGap(4);
                            buttonRow.setVisible(false);
                            Runnable openViewer = () -> Minecraft.getInstance().setScreen(new ScreenshotViewerScreen(viewerIndex, this));

                            buttonRow.addChild(new ButtonNode()
                                    .size(BUTTON_SIZE, CELL_BTN_HEIGHT)
                                    .setIconCenter(ModUiTextures.COPY.getId())
                                    .setOnPress(screenshot::copyToClipboard)
                                    .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.copy"))
                                    );

                            buttonRow.addChild(new ButtonNode()
                                    .size(BUTTON_SIZE, CELL_BTN_HEIGHT)
                                    .setIconCenter(ModUiTextures.TRASH.getId())
                                    .setVariant(ButtonNode.ButtonVariant.DANGER)
                                    .setOnPress(() -> confirmDeleteIndex.set(viewerIndex))
                                    .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.delete"))
                                    );

                            TextureNode texture = new TextureNode()
                                    .setLoadedTextureSupplier(() -> screenshot.getTexture(TEXTURE_MAX_HEIGHT))
                                    .setCornerRadius(6)
                                    .setShowLoader(true)
                                    .fullWidth().top(0).bottom(CELL_FOOTER_HEIGHT);
                            texture.addChild(new ClickOverlayNode(openViewer) {
                                @Override
                                public boolean focusable() {
                                    return true;
                                }

                                @Override
                                public boolean onKeyPress(int keyCode, int modifiers) {
                                    if (UiKeys.isActivationKey(keyCode)) {
                                        openViewer.run();
                                        return true;
                                    }
                                    return false;
                                }
                            }.full());
                            texture.addChild(buttonRow);

                            PositionedNode<?> cell = new RevealOnInteractionCellNode(buttonRow).full();
                            cell.addChild(texture);

                            cell.addChild(new TextNode(screenshot::getName)
                                    .setColorResolver(UiTheme::textMuted)
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
                    .setTitle(UiTranslations.INSTANCE.get("alumite.screenshot.delete_popup.title"))
                    .setDescription(Component.translatable("alumite.screenshot.delete_popup.message_fmt", toDelete.getName()).getString())
                    .setConfirmLabel(UiTranslations.INSTANCE.get("alumite.screenshot.delete_popup.confirm"))
                    .setOnCancel(() -> confirmDeleteIndex.set(-1))
                    .setOnConfirm(() -> {
                        confirmDeleteIndex.set(-1);
                        ScreenshotManager.delete(toDelete);
                    }));
        }

        return root;
    }

    /**
     * Thumbnail cell that shows its action buttons while the pointer is over it or keyboard focus is inside it.
     */
    private static class RevealOnInteractionCellNode extends PositionedNode<RevealOnInteractionCellNode> {

        private final PositionedNode<?> revealedRow;

        RevealOnInteractionCellNode(PositionedNode<?> revealedRow) {
            this.revealedRow = revealedRow;
        }

        @Override
        public void layout(AlumiteGuiRenderer renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
            revealedRow.setVisible(isInteracted(this));
            super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        }

        private static boolean isInteracted(UiNode node) {
            if (node.isHovered() || node.isFocused()) {
                return true;
            }
            for (UiNode child : node.childrenView()) {
                if (isInteracted(child)) {
                    return true;
                }
            }
            return false;
        }
    }
}
