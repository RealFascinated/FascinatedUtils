package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.declare.DeclarativeMountHost;
import cc.fascinated.fascinatedutils.gui.social.components.SocialRootComponent;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class SocialScreen extends WidgetScreen {
    private static final int FOCUS_ADD_FRIEND = 7110;

    private final FWidgetHost host = new FWidgetHost();
    private final DeclarativeMountHost declarativeMountHost;
    private final FOutlinedTextInputWidget addFriendInput;

    private float scrollAccum;
    private boolean preferredPresenceMenuOpen;
    private SocialRootComponent.PresenceHitTest presenceHitTest;

    public SocialScreen() {
        super(Component.translatable("fascinatedutils.social.title"));
        addFriendInput = new FOutlinedTextInputWidget(FOCUS_ADD_FRIEND, 32, 22f,
                () -> Component.translatable("fascinatedutils.social.add_friend_placeholder").getString());
        addFriendInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        Consumer<Boolean> presenceMenuOpenSink = menuOpen -> preferredPresenceMenuOpen = menuOpen;
        declarativeMountHost = new DeclarativeMountHost((viewportWidth, viewportHeight) ->
                SocialRootComponent.view(new SocialRootComponent.Props(viewportWidth, viewportHeight,
                        addFriendInput, () -> Minecraft.getInstance().setScreen(null),
                        presenceMenuOpenSink, hitTest -> presenceHitTest = hitTest)));
        host.setRoot(declarativeMountHost);
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        float deltaSeconds = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = delta / 20f;
        }

        GuiRenderer renderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        renderer.begin(uiWidth, uiHeight);
        host.tickAnimations(deltaSeconds);
        host.layoutAndRender(renderer, 0f, 0f, uiWidth, uiHeight, pointerX, pointerY, deltaSeconds);
        renderer.end();

        host.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
        UiCursorController.apply(minecraftClient.getWindow().handle(), host.pointerCursorAt(pointerX, pointerY));

        if (scrollAccum != 0f) {
            host.dispatchInput(new InputEvent.MouseScroll(pointerX, pointerY, scrollAccum));
            scrollAccum = 0f;
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        if (preferredPresenceMenuOpen && presenceHitTest != null && !presenceHitTest.contains(pointerX, pointerY)) {
            preferredPresenceMenuOpen = false;
        }
        boolean handled = host.dispatchInput(new InputEvent.MousePress(pointerX, pointerY, event.button()));
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseRelease(pointerX, pointerY, event.button()));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAccum += (float) verticalAmount;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (preferredPresenceMenuOpen && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            preferredPresenceMenuOpen = false;
            return true;
        }
        boolean handled = host.dispatchInput(new InputEvent.KeyPress(event.key(), event.scancode(), event.modifiers()));
        if (handled) {
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
        int codepoint = event.codepoint();
        if (codepoint >= 0 && codepoint <= 0xFFFF) {
            return host.dispatchInput(new InputEvent.CharType((char) codepoint));
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        UiCursorController.apply(Minecraft.getInstance().getWindow().handle(), UiPointerCursor.DEFAULT);
        declarativeMountHost.dispose();
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
