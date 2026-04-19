package cc.fascinated.fascinatedutils.systems.hud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleHudState {
    private float positionX = 5f;
    private float positionY = 5f;
    /**
     * Logical-pixel offset paired with {@link #hudAnchor}: inset from the relevant screen edge(s) for corner anchors,
     * or horizontal shift from the anchor line for center/top/bottom/left/right.
     */
    private float anchorOffsetX = 5f;
    /**
     * Logical-pixel offset paired with {@link #hudAnchor}; see {@link #anchorOffsetX}.
     */
    private float anchorOffsetY = 5f;
    private float scale = 1.0f;
    private boolean visible = false;
    private HUDWidgetAnchor hudAnchor = HUDWidgetAnchor.TOP_LEFT;
    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;

    private float committedLayoutWidth = -1f;
    private float committedLayoutHeight = -1f;
    private long lastUpdateTimeMs = 0L;
}
