package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SoundFilterModule extends Module {

    private final List<BooleanSetting> soundToggles = new ArrayList<>();

    public SoundFilterModule() {
        super("Sound Filter", ModuleCategory.GENERAL);
        initSoundToggles();
    }

    private void initSoundToggles() {
        for (Identifier identifier : BuiltInRegistries.SOUND_EVENT.keySet()) {
            String id = identifier.getNamespace() + ":" + identifier.getPath();
            BooleanSetting soundToggle = BooleanSetting.builder()
                    .categoryDisplayKey("fascinatedutils.setting.category.sound_filter")
                    .id(id)
                    .displayName(() -> id)
                    .tooltip(() -> I18n.get("fascinatedutils.module.soundfilter.toggle.description"))
                    .defaultValue(true)
                    .build();
            soundToggles.add(soundToggle);
            addSetting(soundToggle);
        }
    }
}
