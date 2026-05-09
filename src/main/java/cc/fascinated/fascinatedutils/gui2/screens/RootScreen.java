package cc.fascinated.fascinatedutils.gui2.screens;

import cc.fascinated.fascinatedutils.gui2.core.UIScale;
import cc.fascinated.fascinatedutils.gui2.core.UiEvent;
import cc.fascinated.fascinatedutils.gui2.core.UiHost;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.gui2.render.GuiRenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.gui2.theme.impl.DefaultUiTheme;
import cc.fascinated.fascinatedutils.oldgui.screens.WidgetScreen;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/**
 * Centralized gui2 screen root that owns rendering and input override plumbing.
 */
public abstract class RootScreen extends WidgetScreen {
    private final UiHost host = new UiHost();
    private boolean initialized;

    protected RootScreen(Component title) {
        super(title);
    }

    protected UiNode createRootNode() {
        throw new IllegalStateException("RootScreen requires composeRoot(...) or createRootNode() override.");
    }

    protected UiNode composeRoot(UiStateStore stateStore) {
        return createRootNode();
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
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();

        float deltaSeconds = delta / 20f;
        GuiRenderFrame renderFrame = new GuiRenderFrame(drawContext, FascinatedGuiTheme.INSTANCE, uiTheme());
        renderFrame.beginFrame(uiWidth, uiHeight);
        host.tick(deltaSeconds);
        host.layout(0, 0, Math.round(uiWidth), Math.round(uiHeight), renderFrame);
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        host.render(renderFrame, deltaSeconds);
        renderFrame.endFrame();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        ensureInitialized();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        boolean handled = host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, event.button()));
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        ensureInitialized();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, event.button()));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        ensureInitialized();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        host.dispatch(new UiEvent.PointerMove(pointerX, pointerY));
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ensureInitialized();
        host.dispatch(new UiEvent.PointerScroll(UIScale.uiPointerX(), UIScale.uiPointerY(), (float) verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        ensureInitialized();
        if (host.dispatch(new UiEvent.KeyPress(event.key(), event.modifiers()))) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(event);
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
    public void tick() {
        super.tick();
        if (!initialized) {
            return;
        }
        host.tick(1f / 20f);
    }

    @Override
    public void removed() {
        super.removed();
        host.dispose();
        initialized = false;
    }
}
