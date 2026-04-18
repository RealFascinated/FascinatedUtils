package cc.fascinated.fascinatedutils.systems.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModuleCategory {
    HUD("HUD"), MISC("Misc");

    private final String displayName;
}
