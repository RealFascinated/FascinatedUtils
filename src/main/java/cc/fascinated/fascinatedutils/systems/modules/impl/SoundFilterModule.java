package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleDefaults;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

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
                    .categoryDisplayKey("Sounds")
                    .id(id)
                    .displayName(() -> id)
                    .tooltip(() -> "Toggle this sound")
                    .defaultValue(true)
                    .build();
            soundToggles.add(soundToggle);
            addSetting(soundToggle);
        }
    }
}
