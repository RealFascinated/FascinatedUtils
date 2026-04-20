package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.UiFocusIds;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class FWidget {
    private final ArrayList<FWidget> children = new ArrayList<>();
    private FWidget parent;
    @Setter
    private boolean visible = true;
    private float positionX;
    private float positionY;
    private float width;
    private float height;
    private FCellConstraints cellConstraints = FCellConstraints.DEFAULT;

    public final List<FWidget> childrenView() {
        return Collections.unmodifiableList(children);
    }

    public FWidget parent() {
        return parent;
    }

    public boolean visible() {
        return visible;
    }

    public float x() {
        return positionX;
    }

    public float y() {
        return positionY;
    }

    public float w() {
        return width;
    }

    public float h() {
        return height;
    }

    public void setBounds(float positionX, float positionY, float width, float height) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = Math.max(0f, width);
        this.height = Math.max(0f, height);
    }

    public boolean containsPoint(float pointerX, float pointerY) {
        return pointerX >= positionX && pointerY >= positionY && pointerX < positionX + width && pointerY < positionY + height && width > 0f && height > 0f;
    }

    public void addChild(FWidget child) {
        if (child == null) {
            return;
        }
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        child.parent = this;
        if (child.cellConstraints == null) {
            child.cellConstraints = FCellConstraints.DEFAULT;
        }
        children.add(child);
    }

    public void addChild(FWidget child, FCellConstraints constraints) {
        addChild(child);
        if (child != null && constraints != null) {
            child.setCellConstraints(constraints);
        }
    }

    public void removeChild(FWidget child) {
        if (child == null) {
            return;
        }
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    public void clearChildren() {
        for (FWidget child : new ArrayList<>(children)) {
            child.parent = null;
        }
        children.clear();
    }

    public FCellConstraints cellConstraints() {
        return cellConstraints == null ? FCellConstraints.DEFAULT : cellConstraints;
    }

    public void setCellConstraints(FCellConstraints cellConstraints) {
        this.cellConstraints = cellConstraints == null ? FCellConstraints.DEFAULT : cellConstraints;
    }

    public abstract void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight);

    public void render(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (!visible) {
            return;
        }
        renderSelf(graphics, mouseX, mouseY, deltaSeconds);
        for (FWidget child : children) {
            child.render(graphics, mouseX, mouseY, deltaSeconds);
        }
    }

    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
    }

    public boolean fillsHorizontalInRow() {
        return false;
    }

    public boolean fillsVerticalInColumn() {
        return false;
    }

    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return Math.max(width, 0f);
    }

    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return Math.max(height, 0f);
    }

    public int focusId() {
        return UiFocusIds.NO_FOCUS_ID;
    }

    public boolean wantsPointer() {
        return false;
    }

    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        return UiPointerCursor.DEFAULT;
    }

    public float childPointerYOffset() {
        return 0f;
    }

    public boolean clipChildren() {
        return false;
    }

    public boolean mouseDownCapture(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean mouseDown(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean mouseUpCapture(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean mouseUp(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean clickCapture(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean click(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean mouseEnter(float pointerX, float pointerY) {
        return false;
    }

    public boolean mouseLeave(float pointerX, float pointerY) {
        return false;
    }

    public boolean mouseMove(float pointerX, float pointerY) {
        return false;
    }

    public boolean keyDownUnfocused(int keyCode, int modifiers) {
        return false;
    }

    public boolean keyDownCapture(int keyCode, int modifiers) {
        return false;
    }

    public boolean keyDown(int keyCode, int modifiers) {
        return false;
    }

    public boolean charTypedCapture(char character) {
        return false;
    }

    public boolean charTyped(char character) {
        return false;
    }

    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
    }
}
