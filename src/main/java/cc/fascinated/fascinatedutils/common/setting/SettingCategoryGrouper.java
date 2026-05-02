package cc.fascinated.fascinatedutils.common.setting;

import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Derives UI category sections from keys returned by each {@link Setting} instance's {@code getCategoryDisplayKey()}
 * accessor.
 */
public final class SettingCategoryGrouper {

    private SettingCategoryGrouper() {
    }

    /**
     * Settings with no category key (or a blank key), in registration order.
     *
     * @param settings ordered widget or module settings
     * @return unmodifiable view of top-level settings
     */
    public static List<Setting<?>> topLevelInRegistrationOrder(List<Setting<?>> settings) {
        List<Setting<?>> top = new ArrayList<>();
        for (Setting<?> setting : settings) {
            if (isTopLevel(setting)) {
                top.add(setting);
            }
        }
        return Collections.unmodifiableList(top);
    }

    /**
     * Category sections: first occurrence of a non-blank category key defines section order; settings within a section
     * follow registration order.
     *
     * @param settings ordered widget or module settings
     * @return unmodifiable list of {@link SettingCategory} rows for the settings UI
     */
    public static List<SettingCategory> categoriesInRegistrationOrder(List<Setting<?>> settings) {
        LinkedHashMap<String, List<Setting<?>>> byKey = new LinkedHashMap<>();
        for (Setting<?> setting : settings) {
            @Nullable String key = setting.getCategoryDisplayKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            byKey.computeIfAbsent(key, absentKey -> new ArrayList<>()).add(setting);
        }
        List<SettingCategory> out = new ArrayList<>(byKey.size());
        for (Map.Entry<String, List<Setting<?>>> entry : byKey.entrySet()) {
            List<Setting<?>> sectionSettings = entry.getValue();
            out.add(new SettingCategory(entry.getKey(), List.copyOf(sectionSettings)));
        }
        return Collections.unmodifiableList(out);
    }

    private static boolean isTopLevel(Setting<?> setting) {
        @Nullable String key = setting.getCategoryDisplayKey();
        return key == null || key.isBlank();
    }
}
