package cc.fascinated.fascinatedutils.settings;

import lombok.Getter;

@Getter
public class SettingsRegistry {
    public static final SettingsRegistry INSTANCE = new SettingsRegistry();

    private final Settings settings = new Settings();
}
