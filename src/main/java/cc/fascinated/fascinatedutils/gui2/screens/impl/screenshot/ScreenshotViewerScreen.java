package cc.fascinated.fascinatedutils.gui2.screens.impl.screenshot;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.ByteUtils;
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
    private static final int TEXTURE_MAX_HEIGHT = 4096;

    private final int initialIndex;
    private UiState<Integer> indexState;
    private UiState<Integer> listVersion;

    public ScreenshotViewerScreen(int initialIndex) {
        super(Component.translatable("alumite.screenshot.viewer.title"));
        this.initialIndex = initialIndex;
    }

    @Override
    protected UiNode composeContent() {
        List<Screenshot> screenshots = ScreenshotManager.getScreenshots();
        if (screenshots.isEmpty()) {
            Minecraft.getInstance().setScreen(new ScreenshotScreen());
            return new PositionedNode<>().full();
        }

        indexState = stateStore.state("index", initialIndex);
        listVersion = stateStore.state("listVersion", 0);
        UiState<Boolean> confirmDelete = stateStore.state("confirmDelete", false);
        UiState<Boolean> showSendPopup = stateStore.state("showSendPopup", false);

        int clamped = Math.clamp(indexState.get(), 0, screenshots.size() - 1);
        if (clamped != indexState.get()) {
            indexState.set(clamped);
        }

        // Top info: filename + counter
        Screenshot screenshot = screenshots.get(indexState.get());
        PositionedNode<?> topInfo = new PositionedNode<>()
                .fullWidth()
                .top(0)
                .height(TOP_HEIGHT);
        topInfo.addChild(new TextNode(() -> "%s (%s)".formatted(screenshot.getName(), ByteUtils.formatBytes(screenshot.getSize(), 2)))
                .fullWidth()
                .top(7)
                .height(12)
                .setTextAlign(0.5f, 0.5f));
        topInfo.addChild(new TextNode(() -> (indexState.get() + 1) + " / " + screenshots.size())
                .fullWidth()
                .top(22).
                        height(12)
                .setTextAlign(0.5f, 0.5f));

        // Screenshot image
        PositionedNode<?> imageArea = new PositionedNode<>()
                .left(PADDING)
                .right(PADDING)
                .top(TOP_HEIGHT + PADDING)
                .bottom(BAR_HEIGHT + PADDING);
        imageArea.addChild(new TextureNode()
                .setLoadedTextureSupplier(() -> screenshot.getTexture(TEXTURE_MAX_HEIGHT))
                .setCornerRadius(4)
                .setShowLoader(true)
                .naturalSize()
                .full());

        // Bottom bar
        PositionedNode<?> barRow = new PositionedNode<>()
                .left(PADDING)
                .right(PADDING)
                .topRel(0.5f, 0, 0.5f)
                .height(BUTTON_HEIGHT)
                .rowGap(4);
        barRow.addChild(new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.back").getString())
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.BACK.getId())
                .setOnPress(() -> Minecraft.getInstance().setScreen(new ScreenshotScreen()))
                .setRounded(true));
        barRow.addChild(new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.copy").getString())
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.COPY.getId())
                .setOnPress(screenshot::copyToClipboard)
                .setRounded(true));
        barRow.addChild(new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.delete").getString())
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.TRASH.getId())
                .setVariant(ButtonNode.ButtonVariant.DANGER)
                .setOnPress(() -> confirmDelete.set(true))
                .setRounded(true));
        barRow.addChild(new ButtonNode(() -> Component.translatable("alumite.screenshot.viewer.send_to_friend").getString())
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.SHARE.getId())
                .setOnPress(() -> showSendPopup.set(true))
                .setRounded(true));

        barRow.addChild(new SpacerNode());

        barRow.addChild(new ButtonNode()
                .height(BUTTON_HEIGHT).width(NAV_BUTTON_WIDTH)
                .setIconCenter(ModUiTextures.CHEVRON_LEFT.getId())
                .setDisabled(() -> !canNavigate(-1))
                .setOnPress(() -> navigate(-1))
                .setRounded(true));
        barRow.addChild(new ButtonNode()
                .height(BUTTON_HEIGHT).width(NAV_BUTTON_WIDTH)
                .setIconCenter(ModUiTextures.CHEVRON_RIGHT.getId())
                .setDisabled(() -> !canNavigate(1))
                .setOnPress(() -> navigate(1))
                .setRounded(true));

        PanelNode bottomBar = new PanelNode()
                .fullWidth().bottom(0).height(BAR_HEIGHT);
        bottomBar.addChild(barRow);

        StackNode root = new StackNode();
        root.addChild(new RectNode().full().setFillResolver(UiTheme::popupBackdropFill));
        root.addChild(topInfo);
        root.addChild(imageArea);
        root.addChild(bottomBar);

        if (confirmDelete.get()) {
            root.addChild(new ConfirmPopupNode()
                    .setTitle(Component.translatable("alumite.screenshot.delete_popup.title").getString())
                    .setDescription(Component.translatable("alumite.screenshot.delete_popup.message").getString())
                    .setConfirmLabel(Component.translatable("alumite.screenshot.delete_popup.confirm").getString())
                    .setOnCancel(() -> confirmDelete.set(false))
                    .setOnConfirm(() -> {
                        confirmDelete.set(false);
                        deleteScreenshot(screenshots, indexState.get());
                    }));
        }

        if (showSendPopup.get()) {
            root.addChild(new SendScreenshotPopupNode(screenshot, () -> showSendPopup.set(false)));
        }

        return root;
    }

    private boolean canNavigate(int delta) {
        List<Screenshot> screenshots = ScreenshotManager.getScreenshots();
        int next = indexState.get() + delta;
        return next >= 0 && next < screenshots.size();
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
