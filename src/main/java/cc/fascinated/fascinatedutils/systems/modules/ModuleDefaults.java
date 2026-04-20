package cc.fascinated.fascinatedutils.systems.modules;

import cc.fascinated.fascinatedutils.systems.hud.HUDWidgetAnchor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Builder(toBuilder = true) @Accessors(fluent = true)
public class ModuleDefaults {
    private boolean defaultState = false;
}
