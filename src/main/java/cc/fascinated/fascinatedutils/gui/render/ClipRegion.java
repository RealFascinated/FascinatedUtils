package cc.fascinated.fascinatedutils.gui.render;

/**
 * A rectangular clip in whole logical pixels.
 *
 * @param positionX left edge
 * @param positionY top edge
 * @param width     width, never negative
 * @param height    height, never negative
 */
public record ClipRegion(int positionX, int positionY, int width, int height) {

    /**
     * Normalizes negative sizes to zero.
     */
    public ClipRegion {
        width = Math.max(0, width);
        height = Math.max(0, height);
    }

    /**
     * @return the right edge, exclusive
     */
    public int right() {
        return positionX + width;
    }

    /**
     * @return the bottom edge, exclusive
     */
    public int bottom() {
        return positionY + height;
    }

    /**
     * Tests whether a point lies inside the clip.
     *
     * @param pointerX x of the point
     * @param pointerY y of the point
     * @return true when the point is inside
     */
    public boolean contains(float pointerX, float pointerY) {
        return pointerX >= positionX && pointerY >= positionY && pointerX < right() && pointerY < bottom();
    }
}
