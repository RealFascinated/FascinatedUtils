package cc.fascinated.fascinatedutils.gui.renderer;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

@UtilityClass
public class RectCornerRoundMask {

    /**
     * No rounded corners (draws an axis-aligned rectangle when this mask alone is used).
     */
    public static final int NONE = 0;
    public static final int TOP_LEFT = 1;
    public static final int TOP_RIGHT = 2;
    public static final int BOTTOM_LEFT = 4;
    public static final int BOTTOM_RIGHT = 8;
    /**
     * All four corners filleted (default when {@code CORNER_ROUND_MASK} is unset).
     */
    public static final int ALL = TOP_LEFT | TOP_RIGHT | BOTTOM_LEFT | BOTTOM_RIGHT;
    public static final int TOP = TOP_LEFT | TOP_RIGHT;
    public static final int BOTTOM = BOTTOM_LEFT | BOTTOM_RIGHT;
    public static final int LEFT = TOP_LEFT | BOTTOM_LEFT;
    public static final int RIGHT = TOP_RIGHT | BOTTOM_RIGHT;

    /**
     * Whether {@code mask} is one of the five layouts that map to dedicated rounded-rect GPU pipelines.
     *
     * @param mask corner fillet bitmask
     * @return true if {@code mask} is exactly {@link #ALL}, {@link #TOP}, {@link #BOTTOM}, {@link #LEFT}, or
     * {@link #RIGHT}, false for any other bit combination
     */
    public static boolean isPresetMask(int mask) {
        return mask == ALL || mask == TOP || mask == BOTTOM || mask == LEFT || mask == RIGHT;
    }

    /**
     * Pack four per-corner fillet radii (0â€“255 logical pixels each) for the GPU rounded-rect LUT.
     *
     * @param cornerRadius    requested fillet radius when a corner bit is set
     * @param cornerRoundMask bitmask of which corners use {@code cornerRadius}; others are sharp (0)
     * @return packed bytes {@code (topLeft<<24)|(topRight<<16)|(bottomRight<<8)|bottomLeft}
     */
    public static int packedCornerRadiiBytes(float cornerRadius, int cornerRoundMask) {
        int radiusByte = Mth.clamp(Math.round(cornerRadius), 0, 255);
        int topLeft = (cornerRoundMask & TOP_LEFT) != 0 ? radiusByte : 0;
        int topRight = (cornerRoundMask & TOP_RIGHT) != 0 ? radiusByte : 0;
        int bottomRight = (cornerRoundMask & BOTTOM_RIGHT) != 0 ? radiusByte : 0;
        int bottomLeft = (cornerRoundMask & BOTTOM_LEFT) != 0 ? radiusByte : 0;
        return (topLeft << 24) | (topRight << 16) | (bottomRight << 8) | bottomLeft;
    }
}
