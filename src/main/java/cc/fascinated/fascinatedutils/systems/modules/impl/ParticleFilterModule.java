package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ParticleFilterModule extends Module {

    private final List<BooleanSetting> particleToggles = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final Map<ParticleType<?>, BooleanSetting> togglesByType = new IdentityHashMap<>();

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
        BooleanSetting toggle = togglesByType.get(options.getType());
        return toggle != null && !toggle.getValue();
    }

    /**
     * Checks whether the user has switched off the given particle type.
     *
     * @param particleType the particle type about to be created
     * @return true when particles of this type should be suppressed
     */
    public boolean isParticleTypeBlocked(ParticleType<?> particleType) {
        BooleanSetting toggle = togglesByType.get(particleType);
        return toggle != null && !toggle.getValue();
    }

    private void initParticleToggles() {
        for (Identifier identifier : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            String id = identifier.getNamespace() + ":" + identifier.getPath();
            BooleanSetting particleToggle = BooleanSetting.builder().categoryDisplayKey(identifier.getNamespace()).id(id).displayName(() -> id).tooltip(() -> I18n.get("alumite.module.particlefilter.toggle.description")).defaultValue(true).build();
            particleToggles.add(particleToggle);
            togglesByType.put(BuiltInRegistries.PARTICLE_TYPE.getValue(identifier), particleToggle);
            addSetting(particleToggle);
        }
    }
}
