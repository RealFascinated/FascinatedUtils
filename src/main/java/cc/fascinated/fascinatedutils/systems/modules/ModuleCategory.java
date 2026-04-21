package cc.fascinated.fascinatedutils.systems.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModuleCategory {
    GENERAL("General"), HUD("HUD");

    private final String displayName;
}
