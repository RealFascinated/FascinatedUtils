package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.ClipRegion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class UiBounds {
    private int positionX;
    private int positionY;
    private int width;
    private int height;
    @Getter(AccessLevel.NONE)
    private ClipRegion clipRegion;

    public UiBounds(int positionX, int positionY, int width, int height) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public int right() {
        return positionX + width;
    }

    public int bottom() {
        return positionY + height;
    }

    public void set(int nextX, int nextY, int nextWidth, int nextHeight) {
        int clampedWidth = Math.max(0, nextWidth);
        int clampedHeight = Math.max(0, nextHeight);
        if (positionX == nextX && positionY == nextY && width == clampedWidth && height == clampedHeight) {
            return;
        }
        this.positionX = nextX;
        this.positionY = nextY;
        this.width = clampedWidth;
        this.height = clampedHeight;
        this.clipRegion = null;
    }

    public void translate(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) {
            return;
        }
        this.positionX += deltaX;
        this.positionY += deltaY;
        this.clipRegion = null;
    }

    public boolean contains(float pointerX, float pointerY) {
        return pointerX >= positionX && pointerY >= positionY && pointerX < right() && pointerY < bottom();
    }

    public boolean intersects(ClipRegion clip) {
        return positionX < clip.right() && right() > clip.positionX()
                && positionY < clip.bottom() && bottom() > clip.positionY();
    }

    /**
     * Returns these bounds as a clip region. The region is cached until the bounds change.
     *
     * @return the clip region matching the current bounds
     */
    public ClipRegion asClipRegion() {
        if (clipRegion == null) {
            clipRegion = new ClipRegion(positionX, positionY, width, height);
        }
        return clipRegion;
    }
}
