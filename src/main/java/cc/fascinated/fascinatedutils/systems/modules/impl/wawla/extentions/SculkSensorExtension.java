package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
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
            case ACTIVE -> "Phase: Active";
            case COOLDOWN -> "Phase: Cooldown";
            default -> "Phase: Inactive";
        };
        return List.of(phaseLabel, "Signal: " + power);
    }
}
