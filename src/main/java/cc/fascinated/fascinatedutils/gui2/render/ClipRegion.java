package cc.fascinated.fascinatedutils.gui2.render;

/**
 * Axis-aligned clipping region in logical UI coordinates.
 */
public class ClipRegion {
    private final int positionX;
    private final int positionY;
    private final int width;
    private final int height;

    public ClipRegion(int positionX, int positionY, int width, int height) {
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

    public boolean contains(float pointerX, float pointerY) {
        return pointerX >= positionX && pointerY >= positionY && pointerX < right() && pointerY < bottom();
    }

    public ClipRegion intersect(ClipRegion otherRegion) {
        int intersectX = Math.max(positionX, otherRegion.positionX);
        int intersectY = Math.max(positionY, otherRegion.positionY);
        int intersectRight = Math.min(right(), otherRegion.right());
        int intersectBottom = Math.min(bottom(), otherRegion.bottom());
        return new ClipRegion(intersectX, intersectY, Math.max(0, intersectRight - intersectX), Math.max(0, intersectBottom - intersectY));
    }
}
