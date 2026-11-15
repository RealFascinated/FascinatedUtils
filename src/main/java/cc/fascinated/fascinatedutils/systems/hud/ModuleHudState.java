package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleHudState {
    private float positionX = 5f;
    private float positionY = 5f;
    private float anchorOffsetX = 5f;
    private float anchorOffsetY = 5f;
    private float scale = 1.0f;
    private HUDWidgetAnchor hudAnchor = HUDWidgetAnchor.TOP_LEFT;
    /**
     * For the {@code BOTTOM} anchor only: whether the panel hangs left of the centre line (offset measured
     * from its right edge) instead of right of it (offset measured from its left edge).
     */
    private boolean bottomAnchorRightAligned = false;

    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;
    private float committedLayoutWidth = -1f;
    private float committedLayoutHeight = -1f;

    /**
     * Corners (see {@code RectCornerRoundMask}) drawn square because another HUD panel touches them.
     */
    private int squaredCornerMask;
    /**
     * Whether the top padding is dropped because this panel touches the panel above it.
     */
    private boolean trimTopPadding;
    /**
     * Whether the left padding is dropped because this panel touches the panel to its left.
     */
    private boolean trimLeftPadding;
    private float appliedTopTrim;
    private float appliedLeftTrim;
}
