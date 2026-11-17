package cc.fascinated.fascinatedutils.gui.core.overlay;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;

import cc.fascinated.fascinatedutils.gui.core.input.UiEvent;

import cc.fascinated.fascinatedutils.gui.core.UiScale;

import cc.fascinated.fascinatedutils.gui.core.UiHost;

import cc.fascinated.fascinatedutils.gui.core.UiComposer;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Hosts a node tree that is drawn outside any screen, on top of the game or of an open screen. The overlay owns a
 * {@link UiHost} and one reusable renderer. The owner calls {@link #render} every frame the overlay should be
 * visible; an overlay that was not rendered last frame neither draws nor receives input. Register it with
 * {@link UiOverlays} to let it receive mouse buttons that would otherwise reach the game or a screen.
 *
 * <p>The composed content is placed inside a full-canvas root, so content positions itself with absolute constraints
 * such as {@code pos} or {@code leftRel}. The root itself is transparent to the pointer.
 */
@Accessors(fluent = true, chain = true)
public class UiOverlay {
    private static final long NEVER_RENDERED = Long.MIN_VALUE / 2;
    private static final float OFFSCREEN_POINTER = -1_000_000f;
    private static final int MAX_BUTTONS = 32;

    private final UiHost host = new UiHost();
    private AlumiteGuiRenderer renderer;
    private float canvasWidth = 1f;
    private float canvasHeight = 1f;
    private long lastRenderedFrame = NEVER_RENDERED;
    private int capturedButtons;
    /**
     * Whether literal text uses the mod's GUI font.
     */
    @Setter
    private boolean guiFont;
    /**
     * Whether the overlay consumes mouse buttons that land on its nodes.
     */
    @Setter
    private boolean acceptsInput;

    /**
     * Creates an overlay for a composer. The content it returns is recomposed when its state changes and at least once
     * a second; use {@link #recomposeEveryFrame(boolean)} for content that changes every frame.
     *
     * @param composer builds the overlay's content; the returned node is placed inside a full-canvas root
     */
    public UiOverlay(UiComposer composer) {
        host.setComposer(stateStore -> {
            PositionedNode<?> root = new PositionedNode<>().full();
            root.addChild(composer.compose(stateStore));
            return root;
        });
    }

    /**
     * Makes the overlay recompose on every frame, for content that changes every frame.
     *
     * @param recomposeEveryFrame true to recompose each frame
     * @return this overlay
     */
    public UiOverlay recomposeEveryFrame(boolean recomposeEveryFrame) {
        host.setRecomposeEveryFrame(recomposeEveryFrame);
        return this;
    }

    /**
     * Composes, lays out and draws the overlay on a canvas, and updates hover from the pointer.
     *
     * @param graphics      the draw context to submit to
     * @param canvasWidth   canvas width in UI pixels
     * @param canvasHeight  canvas height in UI pixels
     * @param pointerX      pointer X in canvas UI pixels
     * @param pointerY      pointer Y in canvas UI pixels
     * @param deltaSeconds  seconds since the previous frame
     */
    public void render(GuiGraphicsExtractor graphics, float canvasWidth, float canvasHeight, float pointerX, float pointerY, float deltaSeconds) {
        if (renderer == null) {
            renderer = new AlumiteGuiRenderer(graphics, guiFont);
        }
        else {
            renderer.rebind(graphics, guiFont);
        }
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        lastRenderedFrame = UiOverlays.INSTANCE.frame();
        boolean pointerAvailable = !Minecraft.getInstance().mouseHandler.isMouseGrabbed();
        float hoverX = pointerAvailable ? pointerX : OFFSCREEN_POINTER;
        float hoverY = pointerAvailable ? pointerY : OFFSCREEN_POINTER;

        renderer.beginFrame(canvasWidth, canvasHeight);
        host.layout(0, 0, Math.round(canvasWidth), Math.round(canvasHeight), renderer);
        if (acceptsInput) {
            host.dispatch(new UiEvent.PointerMove(hoverX, hoverY));
        }
        host.render(renderer, deltaSeconds);
        renderer.endFrame();
    }

    /**
     * Whether this overlay was rendered during the previous or current frame and so has laid-out nodes to hit.
     *
     * @return true when the overlay is live
     */
    boolean isLive() {
        return UiOverlays.INSTANCE.frame() - lastRenderedFrame <= 1;
    }

    /**
     * Offers a mouse button press or release to the overlay. A press landing on any of its pointer targets is consumed,
     * and so is the matching release.
     *
     * @param button  the GLFW mouse button
     * @param pressed true for a press, false for a release
     * @return true when the overlay consumed the event and it must not reach the game or a screen
     */
    boolean handleMouseButton(int button, boolean pressed) {
        if (!acceptsInput || !isLive() || button < 0 || button >= MAX_BUTTONS) {
            return false;
        }
        int buttonBit = 1 << button;
        float pointerX = canvasPointerX();
        float pointerY = canvasPointerY();
        if (pressed) {
            if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
                return false;
            }
            boolean routed = host.dispatch(new UiEvent.PointerPress(pointerX, pointerY, button));
            boolean consumed = routed || host.isOverHitTarget(pointerX, pointerY);
            if (consumed) {
                capturedButtons |= buttonBit;
            }
            return consumed;
        }
        boolean routed = host.dispatch(new UiEvent.PointerRelease(pointerX, pointerY, button));
        boolean captured = (capturedButtons & buttonBit) != 0;
        capturedButtons &= ~buttonBit;
        return routed || captured;
    }

    /**
     * Offers a scroll to the overlay.
     *
     * @param delta the vertical scroll amount
     * @return true when a node consumed the scroll
     */
    boolean handleScroll(float delta) {
        if (!acceptsInput || !isLive() || Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return host.dispatch(new UiEvent.PointerScroll(canvasPointerX(), canvasPointerY(), delta));
    }

    /**
     * Releases the overlay's nodes and state.
     */
    public void dispose() {
        host.dispose();
    }

    private float canvasPointerX() {
        return UiScale.hiResPointerX() * canvasWidth / Math.max(1, Minecraft.getInstance().getWindow().getWidth());
    }

    private float canvasPointerY() {
        return UiScale.hiResPointerY() * canvasHeight / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
    }
}
