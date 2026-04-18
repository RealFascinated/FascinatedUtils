package cc.fascinated.fascinatedutils.gui.renderer;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * One scissor layer for {@link GuiRenderer}: logical bounds, integer scissor box for {@link
 * net.minecraft.client.gui.GuiGraphicsExtractor#enableScissor}, and deferred {@code post} tasks when the layer pops.
 */
public class Scissor {

    private Scissor() {
    }

    /**
     * Intersect a child rectangle with a parent region, returning a new region with non-negative size.
     *
     * @param parent enclosing region
     * @param clipX  proposed child left
     * @param clipY  proposed child top
     * @param clipW  proposed child width
     * @param clipH  proposed child height
     * @return intersected region clipped to the parent
     */
    public static Region intersect(Region parent, float clipX, float clipY, float clipW, float clipH) {
        float outX = clipX;
        float outY = clipY;
        float outW = clipW;
        float outH = clipH;
        if (outX < parent.x) {
            outW -= parent.x - outX;
            outX = parent.x;
        }
        else if (outX + outW > parent.x + parent.width) {
            outW -= (outX + outW) - (parent.x + parent.width);
        }
        if (outY < parent.y) {
            outH -= parent.y - outY;
            outY = parent.y;
        }
        else if (outY + outH > parent.y + parent.height) {
            outH -= (outY + outH) - (parent.y + parent.height);
        }
        return new Region(outX, outY, Math.max(0f, outW), Math.max(0f, outH));
    }

    /**
     * A single rectangular clip region in logical pixels with matching integer scissor bounds.
     */
    public static final class Region {
        public final float x;
        public final float y;
        public final float width;
        public final float height;
        public final int ix0;
        public final int iy0;
        public final int ix1;
        public final int iy1;
        public final List<Runnable> postTasks = new ArrayList<>();

        public Region(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.ix0 = Mth.floor(x);
            this.iy0 = Mth.floor(y);
            this.ix1 = Mth.ceil(x + width);
            this.iy1 = Mth.ceil(y + height);
        }
    }
}
