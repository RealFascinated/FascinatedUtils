package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.ParticleOptions;
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

    /**
     * Checks whether the user has switched off the given particle's type.
     *
     * @param options the particle about to be created
     * @return true when the particle should be suppressed
     */
    public boolean isParticleBlocked(ParticleOptions options) {
        Identifier key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (key == null) {
            return false;
        }
        String id = key.getNamespace() + ":" + key.getPath();
        return particleToggles.stream().filter(toggle -> toggle.getNameProvider().get().equals(id)).findFirst().map(toggle -> !toggle.getValue()).orElse(false);
    }

    private void initParticleToggles() {
        for (Identifier identifier : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            String id = identifier.getNamespace() + ":" + identifier.getPath();
            BooleanSetting particleToggle = BooleanSetting.builder().categoryDisplayKey("alumite.setting.category.particle_filter").id(id).displayName(() -> id).tooltip(() -> I18n.get("alumite.module.particlefilter.toggle.description")).defaultValue(true).build();
            particleToggles.add(particleToggle);
            addSetting(particleToggle);
        }
    }
}
