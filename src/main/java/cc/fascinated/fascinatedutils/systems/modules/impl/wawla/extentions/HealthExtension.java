package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaEntityExtension;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HealthExtension extends WawlaEntityExtension<LivingEntity> {
    public HealthExtension() {
        super(LivingEntity.class);
    }

    @Override
    public List<String> getExtension(LivingEntity entity) {
        return List.of(format(entity.getHealth()) + "/" + format(entity.getMaxHealth()) + " <color:#ff5555>\u2764</color>");
    }

    private static String format(float value) {
        if (!Float.isFinite(value)) {
            return "0";
        }
        float rounded = Math.round(value * 10f) / 10f;
        return Math.abs(rounded - Math.round(rounded)) < 0.001f
                ? Integer.toString(Math.round(rounded))
                : Float.toString(rounded);
    }
}
