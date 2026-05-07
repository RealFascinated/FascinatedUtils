package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

class SocialAttachButtonWidget extends FWidget {

    private Path pendingPath;
    private final FButtonWidget button = new FButtonWidget(this::handleClick, () -> pendingPath != null ? "\u00D7" : "+", 20f, 1, 1f, 4f, 1f, 4f, 3f);

    SocialAttachButtonWidget() {
        addChild(button);
    }

    Path pendingPath() {
        return pendingPath;
    }

    void clear() {
        pendingPath = null;
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
        button.layout(measure, lx, ly, lw, lh);
    }

    private void handleClick() {
        if (pendingPath != null) {
            pendingPath = null;
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(4);
            filters.put(stack.UTF8("*.png"));
            filters.put(stack.UTF8("*.jpg"));
            filters.put(stack.UTF8("*.jpeg"));
            filters.put(stack.UTF8("*.gif"));
            filters.flip();
            String selected = TinyFileDialogs.tinyfd_openFileDialog("Select Image", "", filters, "Image Files", false);
            if (selected != null) {
                pendingPath = Path.of(selected);
            }
        }
    }
}
