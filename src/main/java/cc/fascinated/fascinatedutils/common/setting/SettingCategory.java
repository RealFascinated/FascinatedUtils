package cc.fascinated.fascinatedutils.common.setting;

import net.minecraft.client.resources.language.I18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public record SettingCategory(String displayNameKey, List<Setting<?>> settings) {

    /**
     * @param displayNameKey Minecraft translation key (or literal) for the section header
     * @param settings       settings in this section, in display order
     */
    public SettingCategory(String displayNameKey, Setting<?>... settings) {
        this(displayNameKey, List.copyOf(Arrays.asList(settings)));
    }

    /**
     * Whether the translated section header matches a search query.
     *
     * @param queryLower the lowercased query
     * @return true when the header exists and contains the query
     */
    public boolean nameMatchesSearch(String queryLower) {
        return displayNameKey != null && !displayNameKey.isBlank() && I18n.get(displayNameKey).toLowerCase(Locale.ROOT).contains(queryLower);
    }

}
