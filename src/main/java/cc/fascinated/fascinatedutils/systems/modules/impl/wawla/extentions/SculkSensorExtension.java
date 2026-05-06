package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;

import java.util.List;

public class SculkSensorExtension extends WawlaBlockExtension<SculkSensorBlock> {
    public SculkSensorExtension(SculkSensorBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        SculkSensorPhase phase = SculkSensorBlock.getPhase(blockState);
        int power = blockState.getValue(SculkSensorBlock.POWER);
        String phaseLabel = switch (phase) {
            case ACTIVE -> I18n.get("alumite.wawla.sculk_sensor.phase.active");
            case COOLDOWN -> I18n.get("alumite.wawla.sculk_sensor.phase.cooldown");
            default -> I18n.get("alumite.wawla.sculk_sensor.phase.inactive");
        };
        return List.of(phaseLabel, I18n.get("alumite.wawla.sculk_sensor.signal", power));
    }
}
