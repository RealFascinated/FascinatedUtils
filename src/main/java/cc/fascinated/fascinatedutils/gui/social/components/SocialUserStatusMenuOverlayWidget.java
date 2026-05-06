package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialUserStatusMenuOverlayWidget extends FWidget {

    private final SocialUserStatusPickerWidget picker;
    private final FWidget panel;
    private final float padding;
    private final SocialUserStatusMenuWidget menu;
    private final Runnable onDismiss;

    public SocialUserStatusMenuOverlayWidget(SocialUserStatusPickerWidget picker, FWidget panel, float padding,
                                             SocialUserStatusMenuWidget menu, Runnable onDismiss) {
        this.picker = picker;
        this.panel = panel;
        this.padding = padding;
        this.menu = menu;
        this.onDismiss = onDismiss;
        addChild(menu);
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
        float menuWidth = menu.preferredWidth(measure);
        float menuHeight = menu.preferredHeight();

        float menuX = picker.x() + picker.w() - menuWidth;
        float minMenuX = panel.x() + padding;
        float maxMenuX = panel.x() + panel.w() - padding - menuWidth;
        if (maxMenuX < minMenuX) {
            menuX = minMenuX;
        } else {
            menuX = Math.max(minMenuX, Math.min(menuX, maxMenuX));
        }

        float menuY = picker.y() + picker.h() + 4f;
        float maxMenuY = panel.y() + panel.h() - padding - menuHeight;
        if (menuY > maxMenuY) {
            menuY = Math.max(panel.y() + padding, maxMenuY);
        }

        menu.layout(measure, menuX, menuY, menuWidth, menuHeight);
    }

    @Override
    public PointerHitKind pointerHitKind() {
        return PointerHitKind.BLOCK;
    }

    @Override
    public void render(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        if (!visible()) {
            return;
        }
        graphics.drawRect(x(), y(), w(), h(), 0x9905050F);
        graphics.absolutePost(() -> menu.render(graphics, frame, deltaSeconds));
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (!menu.containsPoint(pointerX, pointerY)) {
            onDismiss.run();
            return true;
        }
        return false;
    }
}
