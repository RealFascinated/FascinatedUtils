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

    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;
    private float committedLayoutWidth = -1f;
    private float committedLayoutHeight = -1f;
    private long lastUpdateTimeMs = 0L;
}
