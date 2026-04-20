package cc.fascinated.fascinatedutils.systems.hud;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Builder(toBuilder = true) @Accessors(fluent = true)
public class HudDefaults {
    private boolean defaultState = false;
    private HUDWidgetAnchor defaultAnchor = HUDWidgetAnchor.TOP_LEFT;
    private int defaultXOffset = 5;
    private int defaultYOffset = 5;
}
