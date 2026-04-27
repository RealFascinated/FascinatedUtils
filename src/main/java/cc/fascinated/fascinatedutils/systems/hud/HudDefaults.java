package cc.fascinated.fascinatedutils.systems.hud;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Builder(toBuilder = true)
@Accessors(fluent = true)
public class HudDefaults {
    @Builder.Default
    private boolean defaultState = false;
    @Builder.Default
    private HUDWidgetAnchor defaultAnchor = HUDWidgetAnchor.TOP_LEFT;
    @Builder.Default
    private int defaultXOffset = 5;
    @Builder.Default
    private int defaultYOffset = 5;
    @Builder.Default
    private float defaultPadding = 6f;
}
