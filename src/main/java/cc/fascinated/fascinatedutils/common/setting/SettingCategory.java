package cc.fascinated.fascinatedutils.common.setting;

import java.util.Arrays;
import java.util.List;

public record SettingCategory(String displayNameKey, List<Setting<?>> settings) {

    /**
     * @param displayNameKey Minecraft translation key (or literal) for the section header
     * @param settings       settings in this section, in display order
     */
    public SettingCategory(String displayNameKey, Setting<?>... settings) {
        this(displayNameKey, List.copyOf(Arrays.asList(settings)));
    }

}
