package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.gui.theme.impl.DefaultUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public abstract class RootScreen extends WidgetScreen {

    private final UiHost host = new UiHost();
    private boolean initialized;
    protected final UiStateStore stateStore = host.stateStore();

    public RootScreen(Component title) {
        super(title);
    }

    protected UiNode createRootNode() {
        throw new IllegalStateException("RootScreen requires composeContent(...) or createRootNode() override.");
    }

    /**
     * Subclasses override this to compose their screen content. The returned node is placed inside
     * a full-screen root.
     */
    protected UiNode composeContent() {
        return createRootNode();
    }

    /**
     * Final. Wraps {@link #composeContent} with a full-screen root.
     */
    protected final UiNode composeRoot(UiStateStore stateStore) {
        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(composeContent());
        return root;
    }

    /**
     * Reports whether this screen lays out on the fixed HUD canvas instead of the GUI-scale-aware screen canvas. Only
     * screens that edit HUD elements need this, because HUD elements are positioned on the HUD canvas.
     *
     * @return true to use the HUD canvas
     */
    protected boolean usesHudCanvas() {
        return false;
    }

    private float canvasWidth() {
        return usesHudCanvas() ? UIScale.uiWidth() : UIScale.screenWidth();
    }

    private float canvasHeight() {
        return usesHudCanvas() ? UIScale.uiHeight() : UIScale.screenHeight();
    }

    private float canvasPointerX() {
        return usesHudCanvas() ? UIScale.uiPointerX() : UIScale.screenPointerX();
    }

    private float canvasPointerY() {
        return usesHudCanvas() ? UIScale.uiPointerY() : UIScale.screenPointerY();
    }

    protected UiTheme uiTheme() {
        return DefaultUiTheme.INSTANCE;
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        host.setComposer(this::composeRoot);
        initialized = true;
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        ensureInitialized();
        UiThemeRepository.set(uiTheme());
        float uiWidth = canvasWidth();
        float uiHeight = canvasHeight();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();

        float deltaSeconds = delta / 20f;
        GuiRenderer renderFrame = new GuiRenderer(drawContext);
        renderFrame.beginFrame(uiWidth, uiHeight);
        renderFrame.setPointer(pointerX, pointerY);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderFrame);
        // Dispatch PointerMove twice: the first pass triggers any hover-driven visibility
        // changes (e.g. showing overlaid buttons on cell hover); the second pass re-evaluates
        // the hit-leaf against the now-stable tree so those newly-visible nodes receive
        // onPointerEnter and show their hover style correctly.
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        host.render(renderFrame, deltaSeconds);
        renderFrame.endFrame();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        ensureInitialized();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        boolean hitRegion = GuiRenderer.recordPressedRegion(pointerX, pointerY, event.button());
        boolean handled = hitRegion || host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        ensureInitialized();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
        GuiRenderer.fireAndClearPressedRegion(pointerX, pointerY);
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        ensureInitialized();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ensureInitialized();
        host.dispatch(new UiEvent.PointerScroll(canvasPointerX(), canvasPointerY(), (float) verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        ensureInitialized();
        if (host.dispatch(new UiEvent.KeyPress(event.key(), event.modifiers()))) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onEscapePressed();
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Called when escape was not consumed by a focused node. Closes the screen by default.
     */
    protected void onEscapePressed() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ensureInitialized();
        int codepoint = event.codepoint();
        if (codepoint >= 0 && codepoint <= 0xFFFF) {
            return host.dispatch(new UiEvent.CharType((char) codepoint));
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        host.dispose();
        initialized = false;
    }
}
