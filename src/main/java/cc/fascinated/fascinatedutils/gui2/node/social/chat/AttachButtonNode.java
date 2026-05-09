package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

class AttachButtonNode extends ButtonNode {

    AttachButtonNode(UiState<Path> pendingAttachment) {
        super(null);
        setVariant(ButtonVariant.GHOST);
        setLabel("+");
        setOnPress(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(4);
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.jpeg"));
                filters.put(stack.UTF8("*.gif"));
                filters.flip();
                String selected = TinyFileDialogs.tinyfd_openFileDialog("Select Image", "", filters, "Image Files", false);
                if (selected != null) {
                    pendingAttachment.set(Path.of(selected));
                }
            }
        });
    }
}
