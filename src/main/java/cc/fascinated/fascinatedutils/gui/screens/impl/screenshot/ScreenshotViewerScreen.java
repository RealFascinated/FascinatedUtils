package cc.fascinated.fascinatedutils.gui.screens.impl.screenshot;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.util.ByteUtils;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.layout.StackNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.popup.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.display.PanelNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextureNode;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.screenshot.Screenshot;
import cc.fascinated.fascinatedutils.systems.screenshot.ScreenshotManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
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

    /**
     * Creates the viewer.
     *
     * @param initialIndex index of the screenshot to show first
     * @param parent       the screen to return to, or null to close the screen
     */
    public ScreenshotViewerScreen(int initialIndex, @Nullable Screen parent) {
        super(Component.translatable("alumite.screenshot.viewer.title"), parent);
        this.initialIndex = initialIndex;
    }

    @Override
    public void tick() {
        if (ScreenshotManager.getScreenshots().isEmpty()) {
            super.onEscapePressed();
        }
    }

    private UiState<Integer> indexState() {
        return stateStore.state("index", initialIndex);
    }

    /**
     * Current index clamped into the screenshot list, so deletions never leave the viewer past the end.
     */
    private int currentIndex() {
        return Math.clamp(indexState().get(), 0, Math.max(0, ScreenshotManager.getScreenshots().size() - 1));
    }

    @Override
    protected UiNode composeContent() {
        List<Screenshot> screenshots = ScreenshotManager.getScreenshots();
        if (screenshots.isEmpty()) {
            return new PositionedNode<>().full();
        }

        UiState<Boolean> confirmDelete = stateStore.state("confirmDelete", false);

        int index = currentIndex();
        Screenshot screenshot = screenshots.get(index);
        PositionedNode<?> topInfo = new PositionedNode<>()
                .fullWidth()
                .top(0)
                .height(TOP_HEIGHT);
        topInfo.addChild(new TextNode(() -> "%s (%s)".formatted(screenshot.getName(), ByteUtils.formatBytes(screenshot.getSize(), 2)))
                .fullWidth()
                .top(7)
                .height(12)
                .setTextAlign(0.5f, 0.5f));
        topInfo.addChild(new TextNode(() -> (currentIndex() + 1) + " / " + ScreenshotManager.getScreenshots().size())
                .fullWidth()
                .top(22)
                .height(12)
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
                .setFit(TextureNode.FitMode.CONTAIN)
                .full());

        // Bottom bar
        PositionedNode<?> barRow = new PositionedNode<>()
                .left(PADDING)
                .right(PADDING)
                .topRel(0.5f, 0, 0.5f)
                .height(BUTTON_HEIGHT)
                .rowGap(4);
        barRow.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.back"))
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.BACK.getId())
                .setOnPress(() -> super.onEscapePressed())
                );
        barRow.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.copy"))
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.COPY.getId())
                .setOnPress(screenshot::copyToClipboard)
                );
        barRow.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.delete"))
                .height(BUTTON_HEIGHT)
                .setLeftIcon(ModUiTextures.TRASH.getId())
                .setVariant(ButtonNode.ButtonVariant.DANGER)
                .setOnPress(() -> confirmDelete.set(true))
                );

        barRow.addChild(new SpacerNode());

        barRow.addChild(new ButtonNode()
                .height(BUTTON_HEIGHT).width(NAV_BUTTON_WIDTH)
                .setIconCenter(ModUiTextures.CHEVRON_LEFT.getId())
                .setDisabled(() -> !canNavigate(-1))
                .setOnPress(() -> navigate(-1))
                .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.previous"))
                );
        barRow.addChild(new ButtonNode()
                .height(BUTTON_HEIGHT).width(NAV_BUTTON_WIDTH)
                .setIconCenter(ModUiTextures.CHEVRON_RIGHT.getId())
                .setDisabled(() -> !canNavigate(1))
                .setOnPress(() -> navigate(1))
                .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.screenshot.viewer.next"))
                );

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
                    .setTitle(UiTranslations.INSTANCE.get("alumite.screenshot.delete_popup.title"))
                    .setDescription(Component.translatable("alumite.screenshot.delete_popup.message_fmt", screenshot.getName()).getString())
                    .setConfirmLabel(UiTranslations.INSTANCE.get("alumite.screenshot.delete_popup.confirm"))
                    .setOnCancel(() -> confirmDelete.set(false))
                    .setOnConfirm(() -> {
                        confirmDelete.set(false);
                        deleteScreenshot(screenshot);
                    }));
        }

        return root;
    }

    private boolean canNavigate(int delta) {
        int next = currentIndex() + delta;
        return next >= 0 && next < ScreenshotManager.getScreenshots().size();
    }

    private void navigate(int delta) {
        if (canNavigate(delta)) {
            indexState().set(currentIndex() + delta);
        }
    }

    private void deleteScreenshot(Screenshot screenshot) {
        ScreenshotManager.delete(screenshot);
        if (ScreenshotManager.getScreenshots().isEmpty()) {
            super.onEscapePressed();
        }
    }

    @Override
    protected void onEscapePressed() {
        UiState<Boolean> confirmDelete = stateStore.state("confirmDelete", false);
        if (confirmDelete.get()) {
            confirmDelete.set(false);
        }
        else {
            super.onEscapePressed();
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
