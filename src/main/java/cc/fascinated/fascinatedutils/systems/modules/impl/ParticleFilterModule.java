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
public class ParticleFilterModule extends Module {

    private final List<BooleanSetting> particleToggles = new ArrayList<>();

    public ParticleFilterModule() {
        super("Particle Filter", ModuleCategory.GENERAL);
        initParticleToggles();
    }

    private void initParticleToggles() {
        for (Identifier identifier : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            String id = identifier.getNamespace() + ":" + identifier.getPath();
            BooleanSetting particleToggle = BooleanSetting.builder()
                    .categoryDisplayKey("fascinatedutils.setting.category.particle_filter")
                    .id(id)
                    .displayName(() -> id)
                    .tooltip(() -> I18n.get("fascinatedutils.module.particlefilter.toggle.description"))
                    .defaultValue(true)
                    .build();
            particleToggles.add(particleToggle);
            addSetting(particleToggle);
        }
    }
}
