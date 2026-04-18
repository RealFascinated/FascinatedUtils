package cc.fascinated.fascinatedutils.systems.hud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleHudState {
    private float positionX = 5f;
    private float positionY = 5f;
    private float anchorOffsetX = 0.005f;
    private float anchorOffsetY = 0.005f;
    private float scale = 1.0f;
    private boolean visible = false;
    private HUDWidgetAnchor hudAnchor = HUDWidgetAnchor.TOP_LEFT;
    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;

    private float committedLayoutWidth = -1f;
    private float committedLayoutHeight = -1f;
    private long lastUpdateTimeMs = 0L;

    /**
     * Whether anchor offsets are stored as proportional fractions of canvas dimensions.
     * When true, offsets are in the range [0.0, 1.0] relative to canvas width/height.
     */
    private boolean proportionalOffsets = true;

    /**
     * Set to true when loading legacy configs that stored pixel-based offsets.
     * Cleared after the first render frame converts them to proportional values.
     */
    private boolean needsProportionalMigration = false;
}
