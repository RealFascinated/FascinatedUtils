package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.input.UiEvent;
import cc.fascinated.fascinatedutils.gui.core.UiHost;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.UiScale;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.gui.theme.impl.DefaultUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public abstract class RootScreen extends WidgetScreen {

    private final UiHost host = new UiHost();
    private boolean initialized;
    protected final UiStateStore stateStore = host.stateStore();

    @Nullable
    private final Screen parentScreen;

    public RootScreen(Component title) {
        this(title, null);
    }

    /**
     * Creates a screen that returns to a parent screen when escape is pressed.
     *
     * @param title        the screen title
     * @param parentScreen the screen to return to on escape, or null to close the screen
     */
    public RootScreen(Component title, @Nullable Screen parentScreen) {
        super(title);
        this.parentScreen = parentScreen;
    }

    /**
     * Composes this screen's content. Called whenever the host recomposes; the returned node is placed inside a
     * full-screen root.
     *
     * @return the content node, never null
     */
    protected abstract UiNode composeContent();

    private UiNode composeRoot(UiStateStore hostStateStore) {
        UiNode content = composeContent();
        if (content == null) {
            throw new IllegalStateException(getClass().getSimpleName() + ".composeContent() returned null");
        }
        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(content);
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

    /**
     * Reports whether the content must be recomposed on every frame, for screens that show live previews.
     *
     * @return true to recompose each frame instead of when state changes and once a second
     */
    protected boolean recomposesEveryFrame() {
        return false;
    }

    private float canvasWidth() {
        return usesHudCanvas() ? UiScale.uiWidth() : UiScale.screenWidth();
    }

    private float canvasHeight() {
        return usesHudCanvas() ? UiScale.uiHeight() : UiScale.screenHeight();
    }

    private float canvasPointerX() {
        return usesHudCanvas() ? UiScale.uiPointerX() : UiScale.screenPointerX();
    }

    private float canvasPointerY() {
        return usesHudCanvas() ? UiScale.uiPointerY() : UiScale.screenPointerY();
    }

    protected UiTheme uiTheme() {
        return DefaultUiTheme.INSTANCE;
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        host.setComposer(this::composeRoot);
        host.setRecomposeEveryFrame(recomposesEveryFrame());
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
        AlumiteGuiRenderer renderFrame = new AlumiteGuiRenderer(drawContext, true);
        renderFrame.beginFrame(uiWidth, uiHeight);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderFrame);
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        host.render(renderFrame, deltaSeconds);
        renderFrame.endFrame();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        ensureInitialized();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        boolean routed = host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
        return routed || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        ensureInitialized();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        boolean routed = host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
        return routed || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        ensureInitialized();
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        boolean routed = host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        return routed || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ensureInitialized();
        boolean routed = host.dispatch(new UiEvent.PointerScroll(canvasPointerX(), canvasPointerY(), (float) verticalAmount));
        return routed || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
     * Called when escape was not consumed by a focused node. Returns to the parent screen when one was provided,
     * otherwise closes the screen.
     */
    protected void onEscapePressed() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ensureInitialized();
        int codePoint = event.codepoint();
        if (codePoint >= 0 && host.dispatch(new UiEvent.CharType(codePoint))) {
            return true;
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
