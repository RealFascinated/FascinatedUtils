package cc.fascinated.fascinatedutils.systems.config.impl.settings;

import cc.fascinated.fascinatedutils.systems.config.ConfigManager;
import cc.fascinated.fascinatedutils.systems.config.impl.config.FascinatedConfig;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UIStateRepository {
    private final ConfigManager<FascinatedConfig> configManager;

    public Optional<String> loadLastShellContentTabKey() {
        UIState uiState = configManager.getCurrent().uiState();
        String value = uiState != null ? uiState.lastShellContentTabKey() : null;
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
    }

    public void saveLastShellContentTabKey(String tabKey) {
        FascinatedConfig current = configManager.getCurrent();
        configManager.updateAndSave(new FascinatedConfig(current.activeProfileId(), current.globalSettings(), new UIState(tabKey)));
    }
}
