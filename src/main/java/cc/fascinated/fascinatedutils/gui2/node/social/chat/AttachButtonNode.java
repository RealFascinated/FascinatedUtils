package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

public class AttachButtonNode extends PositionedNode {

    private static final int CORNER_RADIUS = 4;

    private final UiState<Path> pendingAttachment;
    private boolean hovered;

    public AttachButtonNode(UiState<Path> pendingAttachment) {
        this.pendingAttachment = pendingAttachment;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onPointerEnter(float pointerX, float pointerY) {
        hovered = true;
        return false;
    }

    @Override
    public boolean onPointerLeave(float pointerX, float pointerY) {
        hovered = false;
        return false;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (pendingAttachment.get() != null) {
            pendingAttachment.set(null);
            return true;
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
                pendingAttachment.set(Path.of(selected));
            }
        }
        return true;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        boolean hasPending = pendingAttachment.get() != null;
        int fillColor = hasPending
                ? (hovered ? renderFrame.theme().dangerFillHover() : renderFrame.theme().dangerFill())
                : (hovered ? renderFrame.theme().buttonFillHover() : renderFrame.theme().buttonFill());
        int borderColor = hovered ? renderFrame.theme().buttonBorderHover() : renderFrame.theme().buttonBorder();
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();

        renderFrame.drawRoundedRect(posX, posY, width, height, CORNER_RADIUS, fillColor);
        renderFrame.drawRoundedRectFrame(posX, posY, width, height, CORNER_RADIUS, borderColor, 0, 1);

        String label = hasPending ? "\u00D7" : "+";
        int textWidth = renderFrame.measureTextWidth(label, false);
        int textX = posX + (width - textWidth) / 2;
        int textY = posY + (height - renderFrame.fontHeight()) / 2;
        renderFrame.drawText(label, textX, textY, renderFrame.theme().textPrimary(), false, false);
    }
}
