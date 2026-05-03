package cc.fascinated.fascinatedutils.systems.modules.impl.memory;

import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import cc.fascinated.fascinatedutils.systems.modules.impl.memory.hud.MemoryHudPanel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class MemoryWidget extends HudHostModule {

    private final EnumSetting<Format> memoryFormat = EnumSetting.<Format>builder().id("memory_format")
            .defaultValue(Format.FULL)
            .valueFormatter(Format::name)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    public MemoryWidget() {
        super("process_memory", "Process Memory", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        addSetting(memoryFormat);
        registerHudPanel(new MemoryHudPanel(this));
    }

    public EnumSetting<Format> memoryFormatSetting() {
        return memoryFormat;
    }

    @Getter
    @AllArgsConstructor
    public enum Format {
        FULL("Full"),
        PERCENT("Percent");

        private final String name;
    }
}
