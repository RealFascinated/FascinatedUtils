package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;

/**
 * Mutable rectangular bounds in logical UI coordinates.
 */
public class UiBounds {
    private int positionX;
    private int positionY;
    private int width;
    private int height;

    public UiBounds(int positionX, int positionY, int width, int height) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public int positionX() {
        return positionX;
    }

    public int positionY() {
        return positionY;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int right() {
        return positionX + width;
    }

    public int bottom() {
        return positionY + height;
    }

    public void set(int nextX, int nextY, int nextWidth, int nextHeight) {
        this.positionX = nextX;
        this.positionY = nextY;
        this.width = Math.max(0, nextWidth);
        this.height = Math.max(0, nextHeight);
    }

    public boolean contains(float pointerX, float pointerY) {
        return pointerX >= positionX && pointerY >= positionY && pointerX < right() && pointerY < bottom();
    }

    public ClipRegion asClipRegion() {
        return new ClipRegion(positionX, positionY, width, height);
    }
}
